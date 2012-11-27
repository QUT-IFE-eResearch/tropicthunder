package edu.qut.acoustics.tropicthunder.portal.service

import edu.qut.acoustics.tropicthunder.domain.*
import groovy.io.*
import groovy.util.*
import java.io.*
import org.apache.commons.io.*
import java.util.*
/*
 * File Harvester Service
 * 
 * <p>
 * This class checks a locally accessible file path for audio files. 
 * 
 * Configuration found at ~/{user.home}/{app.name}/AppConfig.groovy
 * </p>
 * 
 * @author Shilo Banihit<shiloworks@gmail.com> 
 *
 */
class FileHarvesterService {
    def dbService
    def configService
    
    def harvestDirStr
    def checkIntervalSleep    
    def harvestDir 
    def ageCheckMillis              
    def fileExts
    def soxPath
    def dateTimeFormat
    def repoId
    def repoStatQueued
    def soxFields
       
    def init() {                
        harvestDirStr = configService.config.settings.harvester.path
        checkIntervalSleep = configService.config.settings.harvester.checkInterval 
        ageCheckMillis = configService.config.settings.harvester.ageThreshold 
        fileExts = configService.config.settings.harvester.extensions
        soxPath = configService.config.settings.harvester.sox
        dateTimeFormat = configService.config.settings.harvester.dateTimeFormat
        repoId = configService.config.settings.harvester.repoId
        repoStatQueued = configService.config.settings.repository.statQueued
        soxFields = configService.config.settings.harvester.sox_fields
        harvestDir = new File(harvestDirStr)
        log.debug "Using Harvest Directory: $harvestDirStr"     
    }
    
    def exec() {
        walker(harvestDir)
    }
    
    def walker(curDir) {        
        curDir.eachFileMatch FileType.FILES, { FilenameUtils.isExtension(it.toLowerCase(), fileExts) }, {
            def curPath = it.path
            log.debug "Found file:"  + curPath
            processRecording(it)
        }
        curDir.eachFile(FileType.DIRECTORIES) {
           walker(it)
        }
    }        
        
    def processRecording(recording) {
        log.debug "Processing a file:$recording.path"
        def now = System.currentTimeMillis()
        // check if the file path exists in the DB and get last modified date
        def dbRec = dbService.getDBRecordingByFullPath(recording.path)
        if (dbRec != null) {
            log.debug "DB Last Modified:" + dbRec.lastModifiedDate
        }
        log.debug "Local Last modified:" + recording.lastModified()
        def hasAged = now - recording.lastModified() >= ageCheckMillis
        if ((dbRec == null && hasAged ) || (dbRec != null && recording.lastModified() > dbRec.lastModifiedDate && hasAged) ) { 
            processRecording(recording.path, recording.lastModified())
            return true
        } else {
            log.debug "Ignoring recently modified: " + recording.path
        }
        return false
    }
    // Based on 
    // https://isr-eresearch.atlassian.net/browse/ACOUSTICS-27
    // https://isr-eresearch.atlassian.net/wiki/display/fascinator/Metadata+Source+and+Derivation+Rules
    // 
    // Behavior is intended to be 'lightweight' in that projects, sites and sensors can be inserted on the fly
    // and later administered by the librarian.
    // 
    def processRecording(filepath, lastModifiedDate) {
        // determine projectName, siteName, sensor device id based on file path
        def unixpath = FilenameUtils.separatorsToUnix(filepath)
        def fname = FilenameUtils.getName(unixpath)
        def fpath = FilenameUtils.getFullPath(unixpath)
        log.debug "processRecording->FilePath: ${fpath}" 
        // break up path
        def strtok = new StringTokenizer(fpath.replaceAll(harvestDirStr, ""), "/")
        log.debug "processRecording->Number of tokens:" + strtok.countTokens()
        def ctr = 0
        def projectName = null
        def siteName = null
        def deviceId = null
        while (strtok.hasMoreTokens()) {
           switch (ctr) {
               case 0:
                projectName = strtok.nextToken()
                break
               case 1:
                siteName = strtok.nextToken()
                break
               case 2:
                deviceId = strtok.nextToken()
                break
               default:
                strtok.nextToken()
           }
           ctr++
        }
        log.debug "Project:" +projectName
        log.debug "Sitename:" +siteName
        log.debug "deviceId:" +deviceId
        
        def soxdetails = [:]
        def soxdump = [:]
        
        soxParse("${soxPath} --info ${filepath}", soxdump)
        soxParse("${soxPath} --info -D ${filepath}", soxdump, "Length (seconds)")        
        // get what we're interested in...
        soxFields.each {
            if (soxdump.containsKey(it.label)) {
                soxdetails.put(it.key, soxdump[it.label])
            }
        }
        soxdetails.each { label, soxdata ->
             log.debug label + " is " + soxdata
        }        
        def fnameTok = fname.tokenize('.')[0].tokenize('_')
        def startDt = Date.parse(dateTimeFormat, fnameTok[1]+fnameTok[2])
        def endDtCal = startDt.toCalendar()
        endDtCal.add(Calendar.SECOND, Math.round(Float.parseFloat(soxdetails["durationSecs"])))
        def endDt = endDtCal.getTime()
        def recording = [   fileName:(fname),
                filePath:(fpath),
                fullPath:(unixpath),
                repoId:(repoId), 
                repoStat:(repoStatQueued), 
                lastModifiedDate:(lastModifiedDate), 
                deviceId:(deviceId),
                siteName:(siteName),
                projectName:(projectName),
                compression:(FilenameUtils.getExtension(fname)),
                startDt: startDt,
                endDt: endDt
            ]         
        // save site
        log.debug "Saving site...${siteName}"
        dbService.saveSite(siteName, recording) 
        // save project        
        log.debug "Saving project...${projectName}"
        dbService.saveProject(projectName, siteName)
        // save sensor
        log.debug "Saving sensor...${deviceId}"
        dbService.saveSensor(deviceId)
        // save recording
        log.debug "Saving recording...${recording.fileName}"
//        recording.putAll(soxdetails)        
//        dbService.saveRecording(unixpath, recording)
        
        def dbRec = new Recording()
        try {
            dbRec.fileName = fname
            dbRec.filePath=fpath
            dbRec.fullPath=unixpath
            dbRec.repoId=repoId 
            dbRec.repoStat=repoStatQueued 
            dbRec.lastModifiedDate=lastModifiedDate 
            dbRec.deviceId=deviceId
            dbRec.siteName=siteName
            dbRec.projectName=projectName
            dbRec.compression=FilenameUtils.getExtension(fname)
            dbRec.startDt=startDt
            dbRec.endDt=endDt
            dbRec.channels=soxdetails.channels
            dbRec.sampleRate=soxdetails.sampleRate
            dbRec.encFormat=soxdetails.encFormat
            dbRec.durationSecs=Double.parseDouble(soxdetails.durationSecs)
        } catch (Exception e) {
            log.error "Exception!"
            log.error e
        }
        log.debug "Flushing recording..."
        if (!dbRec.save(flush:true)) {
            log.error "Error saving recording..."
            dbRec.errors.each {
                log.error "Error: ${it}"
            }
        }
        log.debug "processRecording->DONE! FilePath: ${unixpath}"
    }
    
    def soxParse(soxcmd, soxdump, maplabel=null) {
        log.debug "Soxcmd: ${soxcmd}"
        Process p_info = soxcmd.execute() 
        p_info.in.eachLine { line ->
            line = line.trim()
            log.debug "sox line:${line}"
            if (maplabel == null) {
                if (line.indexOf(":") > 0) {
                    def label = line.substring(0, line.indexOf(":")).trim()
                    def soxdata = line.substring(line.indexOf(":")+1, line.length()).trim()               
                    // dump each entry, optimize later!
                    soxdump.put(label, soxdata)
                }
            } else {                
                if (line.length() > 0)
                    soxdump.put(maplabel, line)
            }
        }        
    }
}

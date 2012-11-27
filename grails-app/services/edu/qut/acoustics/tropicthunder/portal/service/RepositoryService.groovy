package edu.qut.acoustics.tropicthunder.portal.service

import java.io.*
import groovy.json.*
import java.security.MessageDigest

/**
 * Manages the data repository. 
 * 
 * This class uses The Fascinator repository. Communication currently secured by a token.
 * 
 * 
 * @author Shilo Banihit<shiloworks@gmail.com>
 */

class RepositoryService {
    def configService
    def dbService    
    
    def urlRepo
    def urlPathHarvest
    def urlPathSolr
    def token
    def privateKey
    def publicKey
    def tokenExpiry
    def repoStatPushed
    def repoStatLive
    def metaFileExt
    def configFileExt
    def renderPendingWait
    def renderPendingMaxCheck
    def maxRender
    def pathHeartbeat
    
    def secToken
    def secTokenCreateDt
    def repoAlive
    
    def init() {
        urlRepo = configService.config.settings.fascinator.urlRepo
        urlPathHarvest = configService.config.settings.fascinator.pathHarvest
        privateKey = configService.config.settings.fascinator.privateKey
        publicKey = configService.config.settings.fascinator.publicKey
        tokenExpiry = configService.config.settings.fascinator.tokenExpiry                        
        repoStatPushed = configService.config.settings.repository.statPushed
        repoStatLive = configService.config.settings.repository.statLive
        metaFileExt = configService.config.settings.harvester.metaFileExt
        configFileExt = configService.config.settings.harvester.configFileExt
        urlPathSolr = configService.config.settings.fascinator.pathSolr
        renderPendingWait = configService.config.settings.fascinator.renderPendingWait
        renderPendingMaxCheck = configService.config.settings.fascinator.renderPendingMaxCheck
        maxRender = configService.config.settings.fascinator.maxRender
        pathHeartbeat = configService.config.settings.fascinator.pathHeartbeat
        getHeartbeatStat()
    }
    
    /**
    * Queues the file to the repository for harvesting.
    * 
    * Returns the number of records queued.
    */
    def push() {        
        def ctr = 0
        def pendingRenderCtr = 0
        dbService.getQueued().each {  
            // check if we've hit the limit...
            if (pendingRenderCtr == maxRender) {
                log.debug "Waiting for pending ${maxRender} harvests to complete."
                pull(maxRender, maxRender * renderPendingMaxCheck)                
                pendingRenderCtr = 0
                log.debug "Pending harvests completed, continuing with harvest..."
            }
            // prepare the meta file            
            def metafile = new File(it.fullPath + metaFileExt)
            def recording = it
            log.debug "Processing: ${recording.fileName}"
            // build the metadata file
            def strWriter = new StringWriter()
            def jsonBuilder = new StreamingJsonBuilder(strWriter)
            log.debug "Building JSON..."
            jsonBuilder.metadata {
                fileName  recording.fileName
                filePath  recording.filePath
                fullPath  recording.fullPath
                repoId  recording.repoId
                lastModifiedDate  recording.lastModifiedDate
                deviceId  recording.deviceId
                siteName  recording.siteName
                compression  recording.compression
                startDt  recording.startDt
                endDt  recording.endDt
                channels  recording.channels
                sampleRate  recording.sampleRate
                encFormat  recording.encFormat
                durationSecs  recording.durationSecs                    
            }
            log.debug "Pretty printing JSON to: ${it.fullPath + metaFileExt}"
            metafile.withWriter('UTF-8') {writer ->
                writer.write(JsonOutput.prettyPrint(strWriter.toString()))
            }           
            log.debug "Pushing record to repository..."
            // push to the repository   
            validateSecToken()
            log.debug "Using sectoken:$secToken"
            def jsonRes = getJsonResult(urlRepo, urlPathHarvest, [filepath:recording.fullPath,reharvest:"false",owner:"admin", token:secToken], null)
            log.debug "Analysing response..."
            if (jsonRes.status == "success") {
                log.debug "Successfully pushed to repository, updating recording status..."
                // save the status          
                try {
                    recording.setStorageId("")
                    recording.setRepoStat(repoStatPushed)              
                    log.debug "Flushing record.."
                    if (!dbService.saveRecording(recording)) {
                        recording.errors.each {
                            log.error "Error saving $recording.fileName -> ${it}"
                        }
                    } else {
                        log.debug "Updated record status to 'pushed'..."
                    }
                } catch (Exception e) {
                    log.error e
                }
                // add to the harvest status 
                log.debug "Adding to harvest status list..."
                dbService.addRecHarvestStatus(recording.fileName)
                ctr++
                pendingRenderCtr++
            } else {
                log.error "Error encountered while pushing to the repository: '$jsonRes.errormsg', for recording: $jsonRes.path"
            }
        }
        if (pendingRenderCtr > 0) {
            log.debug "Records have been submitted for harvesting, waiting for completion."
            pull(pendingRenderCtr, pendingRenderCtr * renderPendingMaxCheck )
        }
        return ctr
    }   
    
    /**
     * Gets all pushed records for harvesting and check the 'render-pending' flag.
     * 
    */
    def pull(numRecs, numChecks) {
        def renderedCtr = 0
        def checkCtr = 0
        def statusMap = [harvestStat:null, renderPending:true]
        while (renderedCtr < numRecs && checkCtr < numChecks ) {
            sleep(renderPendingWait)            
            dbService.getPushed().each {
                statusMap.harvestStat = null
                statusMap.renderPending = true
                def recording = it
                getRenderPendingValue(recording.fileName, statusMap)
                if (statusMap.harvestStat != null && statusMap.renderPending == false) {                    
                    // save the status and storage id
                    log.debug "Changing recording status to live: ${recording.fileName}"
                    recording.setStorageId(statusMap.storageId)
                    recording.setRepoStat(repoStatLive)                                                    
                    if (!dbService.saveRecording(recording)) {
                        recording.errors.each {
                            log.error "Error updating:${recording.fileName} --> ${it}"
                        }
                    }
                    renderedCtr++
                    // remove from harvest status list...
                    dbService.removeRecHarvestStatus(recording.fileName)
                    log.debug "ALL DONE: ${recording.fileName}"
                }                
            }
            checkCtr++
            log.debug "pull() wait number: ${checkCtr}, maxWait: ${numChecks}"                        
        }
    }
    
    def getHeartbeatStat() {
        log.debug "Checking repo life..."
        def heartbeat = getJsonResult(urlRepo, pathHeartbeat, null, null)
        repoAlive = heartbeat != null && heartbeat.status == "ok"
        if (repoAlive) {
            log.info "Repo is alive and kicking!"
        } else {
            log.error "Repo is DOWN : ${urlRepo}${pathHeartbeat}"
        }
        return repoAlive
    }
    
    def isRepoAlive() {
        return repoAlive
    }
    
    def getRenderPendingValue(fileName, statusMap) {
        def renderPending = true
        def harvestStat = getJsonResult(urlRepo, urlPathSolr, [q:"(file_name:$fileName AND item_type:object)", wt:"json"], null)
        def storageId = ""
        if (harvestStat.response != null && harvestStat.response.numFound > 0 && harvestStat.response.docs[0] != null) {
            renderPending = Boolean.parseBoolean(harvestStat.response.docs[0]['render-pending'][0])
            storageId = harvestStat.response.docs[0]["storage_id"]
            log.debug "Render-pending for $fileName is: $renderPending"
            log.debug "Storage ID for $fileName is: $storageId"
        } else {
            log.debug "Render-pending not available for $fileName"
        }
        statusMap.harvestStat = harvestStat
        statusMap.renderPending = renderPending
        statusMap.storageId = storageId
    }
    
    /**
     * Get JSON document from Fascinator URL. 
     * 
     */
    def getJsonResult(uri, path, query, jsonClos) {
        def retval = null
        try {
            withHttp(uri:uri) {
                get(path:path, query:query, contentType:'application/json') { resp, json ->
                    log.debug "$path, status: $resp.status" 
                    json.each {
                        log.debug it
                    }
                    retval = json
                }
            }
        } catch (Exception e) {
            log.error e
        }
        return retval
    }
    
    /**
     * Check the validity of the token used for Fascinator communication
     * 
    */
    def validateSecToken() {       
        log.debug "Validating security token..."
        if (secTokenCreateDt == null || (System.currentTimeMillis() >= secTokenCreateDt.getTime() + tokenExpiry )) {
            createSecToken(new Date())
        } else {
            log.debug "Using previously created security token..."
        }
    }
    
    /**
     * Create the security token for Fascinator communication
    */
    def createSecToken(tokenDt) {
        log.debug "Creating new security token..."
        secTokenCreateDt = tokenDt
        def timestamp = secTokenCreateDt.format(configService.config.settings.fascinator.timestampFormat)
        def userTsPair = "${configService.config.settings.fascinator.username}:${timestamp}"
        def tokenSeed = "${userTsPair}:${configService.config.settings.fascinator.privateKey}".toString()
        def hash = MessageDigest.getInstance("MD5").digest(tokenSeed.getBytes("UTF-8")).encodeHex().toString()
        secToken = "${userTsPair}:${configService.config.settings.fascinator.publicKey}:${hash}".toString()
    }        
}

package edu.qut.acoustics.tropicthunder.portal.service

import edu.qut.acoustics.tropicthunder.domain.*
import edu.qut.acoustics.tropicthunder.domain.ConfigPortal
import com.mongodb.*
import org.apache.commons.io.*

/**
 *
 *  Tropic Thunder - DB services 
 *  
 * @author Shilo Banihit shiloworks@gmail.com
 */

class DbService {
    static transactional = false
    static outCol = "projectStats"
        
    def mongo
    def configService
    
    def init() {  
    }
    
    /**
     * 
     * USE WITH CAUTION: clears all collections, except config* and system*
     *
     */
    def clearData() {
        log.info "WARNING: Clearing data on db..."
        def db = mongo.getDB(configService.mongo.databaseName)
        db.getCollectionNames().each {           
           if (!it.startsWith("system") && !it.startsWith("config")) {
               log.info "Clearing data in collection: " + it
               db.getCollection(it).remove([] as BasicDBObject)
           }
        }
    }
            
    def getConfig(configDate, endDate) {   
        return ConfigPortal.collection.findOne([startEffectiveDt : ["\$lte": configDate ], endEffectiveDt:["\$lte":endDate]] as BasicDBObject)
    }
    
    def insertConfig(startDate, dtForever, configSrc, settings) {
        ConfigPortal.collection.insert([startEffectiveDt:(startDate), endEffectiveDt:(dtForever), source:(configSrc), settings:(settings)] as BasicDBObject)
    }
                     
    def getDBRecordingByFullPath(filepath) {        
        return Recording.findByFullPath(FilenameUtils.separatorsToUnix(filepath))                
    }
    
    def getQueued() {
        return Recording.findAllByRepoStat(configService.config.settings.repository.statQueued)        
    }
    
    def getPushed() {
        return Recording.findAllByRepoStat(configService.config.settings.repository.statPushed)
    }
    
    def saveSite(siteName, recording) {        
//        Site.collection.findAndModify([name:siteName] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, ['\$set':[name:(siteName)],'$addToSet':[recordings:(recording)] ] as BasicDBObject, true, true)
        def site = Site.findByName(siteName)
        if (site == null) {
            log.debug "Inserting new site..."
            site = new Site(name:siteName)
        } else {
            log.debug "Existing site..."
        }
        log.debug "Flushing site..."
        if (!site.save(flush:true)) {
            site.errors.each {
                log.error "Error saving site: ${it}" 
            }
        } else {
            log.info "Saved site:${siteName}"
        }
    }
    
    def saveProject(projectName, siteName) {        
        Project.collection.findAndModify([name:(projectName)] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, ['\$set':[name:(projectName)],'\$addToSet':[siteNames:(siteName)]] as BasicDBObject, true, true)       
    }
    
    def saveSensor(sensorId) {        
//        Sensor.collection.findAndModify([deviceId:sensorId] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, [deviceId:(sensorId)] as BasicDBObject, true, true )
        def sensor = Sensor.findByDeviceId(sensorId)
        if (sensor == null) {
            log.debug "Creating new sensor...${sensorId}"
            sensor = new Sensor(deviceId:sensorId)
            if (!sensor.save(flush:true)) {
                log.error "Error saving sensor..."
                sensor.errors.each {
                    log.error "Errors: ${it}"
                }
            } else {
                log.info "Saved sensor:${sensorId}"
            }
        } 
    }
    
    def saveRecording(unixpath, recording) {        
        Recording.collection.findAndModify([fullPath:unixpath] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, recording as BasicDBObject, true, true)                                
    }
    
    def saveRecording(recording) {
        return recording.save(flush:true)
    }
    
    def updateStats() {        
        def reduceFunc = "function(key, vals) {var reduced={count:0}; vals.forEach(function(v){reduced.count+=v.count;}); return reduced;}"        
        def mr = null
        mr = Recording.collection.mapReduce("function(){emit({projectName:this.projectName,year:this.startDt.getFullYear()}, {count:1});}", reduceFunc, outCol , MapReduceCommand.OutputType.MERGE, [repoStat : (configService.config.settings.repository.statLive)] as BasicDBObject )
        log.debug ("Updating year Stats, raw status: " + mr.getRaw())
        mr = Recording.collection.mapReduce("function(){emit({projectName:this.projectName,month:this.startDt.getMonth()}, {count:1});}", reduceFunc, outCol, MapReduceCommand.OutputType.MERGE, [repoStat : (configService.config.settings.repository.statLive)] as BasicDBObject )
        log.debug ("Updating monthly Stats, raw status: " + mr.getRaw())
        mr = Recording.collection.mapReduce("function(){emit({projectName:this.projectName,date:this.startDt.getDay()}, {count:1});}", reduceFunc, outCol, MapReduceCommand.OutputType.MERGE, [repoStat : (configService.config.settings.repository.statLive)] as BasicDBObject )
        log.debug ("Updating daily Stats, raw status: " + mr.getRaw())
        mr = Recording.collection.mapReduce("function(){emit({projectName:this.projectName,hour:this.startDt.getHours()}, {count:1});}", reduceFunc, outCol, MapReduceCommand.OutputType.MERGE, [repoStat : (configService.config.settings.repository.statLive)] as BasicDBObject )
        log.debug ("Updating hourly Stats, raw status: " + mr.getRaw())
    }        
    
    def getProjStats(name) {
        return ProjectStats.collection.find(["_id.projectName": (name)] as BasicDBObject)
    }
    
    def clearStats() {
        ProjectStats.collection.remove([] as BasicDBObject)
    }
    
    def setToHarvesting() {
        initHarvestStatus()
        def query = [running:false]
        def op = [running:true, startDt:new Date(), currentRecordings:[]]
        return HarvestStatus.collection.findAndModify(query as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false,  op as BasicDBObject, true, true)
    }
    
    def setToHarvestComplete() {
        def query = [running:true]
        def op = [running:false, endDt:new Date(), currentRecordings:[]]
        return HarvestStatus.collection.findAndModify(query as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, op as BasicDBObject, true, true)
    }
    
    def getHarvestStatus() {
        initHarvestStatus()
        return HarvestStatus.collection.findOne([running:true] as BasicDBObject)
    }
    
    def initHarvestStatus() {
        def hStat = HarvestStatus.collection.findOne([] as BasicDBObject)
        if (hStat == null) {
            HarvestStatus.collection.insert([running:false, startDt:new Date()])
            log.info "Inserted new harvest status document."
        }
    }
    
    def addRecHarvestStatus(recName) {
        def query = [running:true]
        def op = ['\$addToSet':[currentRecordings:(recName)]]
        return HarvestStatus.collection.update(query as BasicDBObject, op as BasicDBObject)
    }
    
    def removeRecHarvestStatus(recName) {
        def query = [running:true]
        def op = ['\$pull':[currentRecordings:(recName)]]
        return HarvestStatus.collection.update(query as BasicDBObject, op as BasicDBObject)
    }
}

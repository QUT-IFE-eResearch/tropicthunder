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
        
    def mongo
    def configService

    def colConfig
    def colRecordings
    def colProjects
    def colSensors
    def colSites
    
    def init() {
        
        colRecordings = Recording.collection
        colProjects = Project.collection
        colSensors = Sensor.collection
        colSites = Site.collection
        colConfig = ConfigPortal.collection
        colProjects = Project.collection
        
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
        return colConfig.findOne([startEffectiveDt : ["\$lte": configDate ], endEffectiveDt:["\$lte":endDate]] as BasicDBObject)
    }
    
    def insertConfig(startDate, dtForever, configSrc, settings) {
        colConfig.insert([startEffectiveDt:(startDate), endEffectiveDt:(dtForever), source:(configSrc), settings:(settings)] as BasicDBObject)
    }
                     
    def getDBRecordingByFullPath(filepath) {        
        return Recording.findByFullPath(FilenameUtils.separatorsToUnix(filepath))                
    }
    
    def getQueued() {
        return Recording.findAllByRepoStat(configService.config.settings.repository.statQueued)        
    }
    
    def saveSite(siteName, recording) {        
        colSites.findAndModify([name:siteName] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, ['\$set':[name:(siteName)],'$addToSet':[recordings:(recording)] ] as BasicDBObject, true, true)
    }
    
    def saveProject(projectName, siteName) {        
        colProjects.findAndModify([name:(projectName)] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, ['\$set':[name:(projectName)],'\$addToSet':[siteNames:(siteName)]] as BasicDBObject, true, true)       
    }
    
    def saveSensor(sensorId) {        
        colSensors.findAndModify([deviceId:sensorId] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, [deviceId:(sensorId)] as BasicDBObject, true, true )
    }
    
    def saveRecording(unixpath, recording) {        
        colRecordings.findAndModify([fullPath:unixpath] as BasicDBObject, [] as BasicDBObject, [sort:[priority:-1]] as BasicDBObject, false, recording as BasicDBObject, true, true)                                
    }
    
    def saveRecording(recording) {
        recording.save(flush:true, failOnError:true)
    }
}

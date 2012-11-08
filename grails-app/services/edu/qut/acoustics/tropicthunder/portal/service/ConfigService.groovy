package edu.qut.acoustics.tropicthunder.portal.service

/**
 *
 *  Tropic Thunder - Configuration Service
 *  
 * @author Shilo Banihit<shiloworks@gmail.com>
 */
class ConfigService {

    static transactional = false
    static dtForever = Date.parse("MMddyyyy", "12319999")
    static configSrc = "Seeded-ConfigService"
    
    def grailsApplication
    def dbService
    
    def config
    def repository    
    def harvester
    def fascinator    
    def mongo            
    
    
    def init() {
        readBaseConfig()
    }
        
    def loadCurrent() {
        log.debug "Loading current configuration from DB"
        def configDate = new Date()
        config = dbService.getConfig(configDate, dtForever)
        if (config == null) {
            log.debug "Configuration not found on DB, syncing loaded config..."
            config = flushToDB(configDate)
        } else {
            log.debug "Using configuration: " + config
        }        
    }
    
    def flushToDB(startDate) {        
        log.debug "Saving configuration to DB."
        def settings = [:]
        settings.put("repository", repository)
        settings.put("harvester", harvester)
        settings.put("fascinator", fascinator)
        
        //config = new Config(startEffectiveDt:startDate, endEffectiveDt:dtForever, source:configSrc, settings:settings)
        dbService.insertConfig(startDate, dtForever, configSrc, settings)
        return dbService.getConfig(startDate, dtForever)
    }
    
    def readBaseConfig() {
        log.debug "Loading packaged configuration from application."
        repository = grailsApplication.config.repository.flatten()
        harvester = grailsApplication.config.harvester.flatten()
        fascinator = grailsApplication.config.fascinator.flatten()
        mongo = grailsApplication.config.grails.mongo.flatten()
        log.debug "Harvester configuration : " + harvester
        log.debug "Repository configuration : " + repository
        log.debug "Fascinator configuration : " + fascinator
    }
}

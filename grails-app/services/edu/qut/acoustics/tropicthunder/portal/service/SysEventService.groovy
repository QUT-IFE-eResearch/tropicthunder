package edu.qut.acoustics.tropicthunder.portal.service

import edu.qut.acoustics.tropicthunder.domain.*
import grails.util.GrailsUtil

/**
 *
 *  Tropic Thunder - System Event handler
 *  
 * @author Shilo Banihit shiloworks@gmail.com
 */

class SysEventService {
    static transactional = false
    
    def mongo
    def dbService
    def configService
    
    def fromTTDoWithApplicationContext = { applicationContext ->
        log.debug "TropicThunder:doWithApplicationContext()"                

        configService.init()
        dbService.init()
        
        configService.loadCurrent()       
        if (GrailsUtil.environment == "development") 
            initDev()
            
       
    }
    
    def fromTTOnShutdown = { event ->
        log.debug "TropicThunder:onShutdown()"
    }
    
    def initDev() {        
        dbService.clearData()
        seedDev()
    }
    
    def seedDev = {  
    }
}

// configuration for plugin testing - will not be included in the plugin zip
environments {
    development {
        repository {            
            statQueued = "queued"
            statPushed = "pushed"            
            statPending = "pending"
            statLive = "live"
            statDeleted = "deleted"
        }
        harvester {
            repoId = "tropic-thunder"            
            path = "C:/Users/banihit/Work/Projects/temp/harvest/"
            ageSecsThreshold = 60
            checkIntervalSecs = 60            
            extensions = ["wav", "mp3"]
            dateTimeFormat = "YYYYMMDDHHmmss"
            sox = "C:/Users/banihit/Work/tools/sox-14-4-0/sox.exe"
            sox_fields = [ 
                ["label":"Channels", "key":"channels"], 
                ["label":"Sample Rate", "key":"sampleRate"], 
                ["label":"Sample Encoding", "key":"encFormat"],
                ["label":"Length (seconds)", "key":"durationSecs"]
            ]
        }
        fascinator {
            urlRepo = ""
            urlHarvest = ""
            privateKey = "Secret1253232"
            publicKey = "randomGibberish"
            username = "admin"            
            expiry = "400"
        }
    }        
}
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'

    warn   'org.mortbay.log'
    
    debug  'grails.app.services.edu.qut.acoustics.tropicthunder.portal.service',
           'grails.app.services.edu.qut.acoustics.tropicthunder.domain',
           'grails.app.services.edu.qut.acoustics.tropicthunder.portal.controller'
}

class TropicThunderGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [mongodb:"1.0.0.GA", rest:"0.7"]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "TropicThunder Plugin" // Headline display name of the plugin
    def author = "Shilo Banihit"
    def authorEmail = "shiloworks@gmail.com"
    def description = '''\
TropicThunder is a portal platform to provide users with the ability to access, annotate and analyse bio-acoustic recordings.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/tropic-thunder"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    // services
    def sysEventService
    def databaseService
    
    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // Implement runtime spring config (optional)
        log.debug "TropicThunder configuring Spring runtime..."
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // Implement post initialization spring config (optional)
        log.debug "TropicThunder calling sysEventService..."
        sysEventService = applicationContext.getBean("sysEventService")
        sysEventService.fromTTDoWithApplicationContext(applicationContext)
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
        sysEventService.fromTTOnShutdown(event)
    }
}

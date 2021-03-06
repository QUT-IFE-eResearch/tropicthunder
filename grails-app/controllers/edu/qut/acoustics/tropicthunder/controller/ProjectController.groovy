package edu.qut.acoustics.tropicthunder.controller

import grails.converters.*
import edu.qut.acoustics.tropicthunder.domain.*

class ProjectController {

    def dbService
    
    def list() {
       response.setHeader("Cache-Control", "no-store")       
       def queryStr = request.getQueryString()
       def sortHdr = "sort("
       def offsetHdr = "items="
       def retVal = []
       def range = request.getHeader("Range")
       def offsetFld = null
       def maxFld = null
       if (range != null) {           
           offsetFld = Integer.parseInt(range.substring(offsetHdr.size(), range.indexOf("-")))
           maxFld = Integer.parseInt(range.substring(range.indexOf("-")+1))           
       }
      
       if (queryStr != null) {
            def sortIdx = queryStr.indexOf(sortHdr)
            if (sortIdx > -1) { 
                def sortStr = queryStr.substring(sortIdx+sortHdr.size(), queryStr.lastIndexOf(")"))
                def sortFld = sortStr.substring(1)
                def orderFld = sortStr.substring(0,1).equals("+") ? "asc" : "desc"
                if (offsetFld != null && maxFld != null)
                    retVal = Project.list(sort:sortFld, order:orderFld,max:maxFld, offset:offsetFld)
                else
                    retVal = Project.list(sort:sortFld, order:orderFld)
            } 
       } else {
           if (offsetFld != null && maxFld != null)
            retVal = Project.list(max:maxFld, offset:offsetFld)
           else
            retVal = Project.list()
       }
       
       render retVal as JSON
    }
    
    def get() {
       def project = Project.get(params.id)
       render project as JSON
    }
    
    def add() {
        def projectJson = JSON.parse(request)
        def project = new Project(projectJson)
        def retval = ["status":"ok", "message":""]
        if (!project.save(flush:true)) {
            retval.status = "nok"
            project.errors.each {
                log.error "Failed to add project: $it"
                retval.message += it
            }
        }        
        dbService.clearStats()
        dbService.updateStats()
        render retval as JSON
    }
    
    def update() {
        log.debug "Updating Project..."
        def projectJson = JSON.parse(request)
        log.debug "Project JSON:${projectJson}"
        def project = Project.get(projectJson.id)
        project.name = projectJson.name
        project.title = projectJson.title
        project.owner = projectJson.owner
        project.contact = projectJson.contact
        project.description = projectJson.description
        project.mapLocation = projectJson.mapLocation
        project.participants = projectJson.participants
        if (!project.save(flush:true)) {
            project.errors.each {
                log.error "Error saving project:"
                log.error it
            }
        }    
        dbService.clearStats()
        dbService.updateStats()
        render project as JSON
    }
    
    def delete() {
        def retval = ["status":"ok"]
        def project = Project.get(params.id)
        if (project) {
            project.delete()
        } else {
            retval.status = "nok"
        }               
        dbService.clearStats()
        dbService.updateStats()
        render retval as JSON
    }
}

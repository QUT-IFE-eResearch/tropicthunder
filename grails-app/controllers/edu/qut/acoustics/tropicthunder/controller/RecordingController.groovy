package edu.qut.acoustics.tropicthunder.controller

import grails.converters.*
import edu.qut.acoustics.tropicthunder.domain.*

class RecordingController {

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
//                if (offsetFld != null && maxFld != null)
//                    retVal = Recording.list(sort:sortFld, order:orderFld,max:maxFld, offset:offsetFld)
//                else
                    retVal = Recording.list(sort:sortFld, order:orderFld)
            } 
       } else {
//           if (offsetFld != null && maxFld != null)
//            retVal = Recording.list(max:maxFld, offset:offsetFld)
//           else
            retVal = Recording.list()
       }
       
       render retVal as JSON
    }
    
    def get() {
       def rec = Recording.get(params.id)
       render rec as JSON
    }
    
    def add() {
        def recJson = JSON.parse(request)
        def rec = new Recording(recJson)
        def retval = ["status":"ok", "message":""]
        if (!rec.save(flush:true)) {
            retval.status = "nok"
            rec.errors.each {
                log.error "Failed to add recording: $it"
                retval.message += it
            }
        }        
        dbService.clearStats()
        dbService.updateStats()
        render retval as JSON
    }
    
    def update() {
        log.debug "Updating Recording..."
        def recJson = JSON.parse(request)
        log.debug "Rec JSON:${recJson}"
        def rec = Recording.get(recJson.id)
        rec.fileName = recJson.fileName
        rec.projectName = recJson.projectName
        rec.siteName = recJson.siteName
        rec.deviceId = recJson.deviceId
        rec.repoStat = recJson.repoStat        
        if (!rec.save(flush:true)) {
            rec.errors.each {
                log.error "Error saving recording:"
                log.error it
            }
        }
        dbService.clearStats()
        dbService.updateStats()
        render rec as JSON
    }
    
    def delete() {
        def retval = ["status":"ok"]
        def rec = Recording.get(params.id)
        if (rec) {
            rec.delete()
        } else {
            retval.status = "nok"
        }                
        dbService.clearStats()
        dbService.updateStats()
        render retval as JSON
    }
}

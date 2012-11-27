package edu.qut.acoustics.tropicthunder.controller

import grails.converters.*
import edu.qut.acoustics.tropicthunder.domain.*

class ProjectController {

    def list() {
       response.setHeader("Cache-Control", "no-store")       
       render Project.list(params) as JSON
    }
    
    def get() {
       def project = Project.get(params.id)
       render project as JSON
    }
    
    def add() {
        def projectJson = JSON.parse(request)
        def project = new Project(projectJson)
        if (!project.save(flush:true)) {
            project.errors.each {
                log.error "Failed to add project: $it"
            }
        }
    }
    
    def update() {
        def projectJson = JSON.parse(request)
        def project = Project.get(params.id)
        
    }
    
    def delete() {
        def project = Project.get(params.id)
        project.delete()
    }
}

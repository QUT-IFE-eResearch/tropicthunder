// environment specific settings
environments {
    development {        
        grails {
            mongo {
                username = ""
                password = ""
                databaseName = ""
            }            
        }
        dataSource {
            
        }
    }
    test {
        dataSource {
                
                
        }
    }
    production {
        dataSource {
                
                
        }
    }
}

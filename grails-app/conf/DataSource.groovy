// environment specific settings
environments {
    development {        
        grails {
            mongo {
                username = "tt_user"
                password = "G3RRONIzQc"
                databaseName = "tt_dev"
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

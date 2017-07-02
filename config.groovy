environments {
    //--------------------------------------------------------------------------------------------------------
    // PROD CONFIGURATION
    //--------------------------------------------------------------------------------------------------------
    production {
      version = '1.0'
      host = 'localhost'
      port = 8089
      baseDir = new File(System.getProperty('redboxApiConfig')).getParent() + '/'
      apiWrapper = "scripts/MintApiWrapper.groovy"
      buildRoute = "scripts/buildRouter.groovy"
      mintBaseUrl = "http://localhost:9001/solr/fascinator/select/"
      upload {
        procdir = '/tmp/csv_todo'
        donedir = '/tmp/csv_done'
      }
      mapping {
        Parties_Groups {
          idColumn = 'id' // value used for searching
          idSearchField = 'ID' // the Solr field search query
          fieldValidation = ['id', 'Name', 'Phone'] // used to validate if the record is valid for this package type, all attributes must exist
          attachments = ['metadata'] // attachments created for each record
          metadata {
            template = 'templates/parties_groups_metadata.json'
            name = 'metadata.json'
            mapping = [
              'id':['ID'],
              'Name':['Name'],
              'Description':['Description'],
              'Email':['Email'],
              'Phone':['Phone'],
              'Parent_Group_ID': ['Parent_Group_ID'],
              'URI': ['URI'],
              'NLA_Party_Identifier': ['NLA_Party_Identifier'],
              'Homepage':['Homepage']
            ]
            scripts = [
              'id': [scriptPath: 'scripts/dc_identifier.groovy']
            ]
          }
        }
        Parties_People {
          idColumn = 'id'
          idSearchField = 'ID'
          fieldValidation = ['id', 'Given_Name', 'Other_Names', 'Family_Name', 'Email']
          attachments = ['metadata']
          metadata {
            template = 'templates/parties_people_metadata.json'
            name = 'metadata.json'
            mapping = [
              'id': ['ID'],
              'Given_Name': ['Given_Name'],
              'Other_Names': ['Other_Names'],
              'Family_Name': ['Family_Name'],
              'Pref_Name': ['Pref_Name'],
              'Honorific': ['Honorific'],
              'Email': ['Email'],
              'Job_Title': ['Job_Title'],
              'GroupID_1': ['GroupID_1'],
              'GroupID_2': ['GroupID_2'],
              'GroupID_3': ['GroupID_3'],
              'ANZSRC_FOR_1': ['ANZSRC_FOR_1'],
              'ANZSRC_FOR_2': ['ANZSRC_FOR_2'],
              'ANZSRC_FOR_3': ['ANZSRC_FOR_3'],
              'URI': ['URI'],
              'NLA_Party_Identifier': ['NLA_Party_Identifier'],
              'ResearcherID': ['ResearcherID'],
              'openID': ['openID'],
              'Personal_URI': ['Personal_URI'],
              'Personal_Homepage': ['Personal_Homepage'],
              'Staff_Profile_Homepage': ['Staff_Profile_Homepage'],
              'Description': ['Description']
            ]
            scripts = [
              'id': [scriptPath: 'scripts/dc_identifier.groovy']
            ]
          }
        }
        Activities {
          idColumn = 'id'
          idSearchField = 'ID'
          fieldValidation = ['Title', 'Name', 'Existence_Start', 'Existence_End']
          attachments = ['metadata']
          metadata {
            template = 'templates/activities_metadata.json'
            name = 'metadata.json'
            mapping = [
              "ID": ["ID"],
              "Title": ["Title"],
              "Name": ["Name"],
              "Type": ["Type"],
              "Existence_Start": ["Existence_Start"],
              "Existence_End": ["Existence_End"],
              "Description": ["Description"],
              "Primary_Investigator_ID": ["Primary_Investigator_ID"] ,
              "Investigators": ["Investigators"],
              "Website": ["Website"],
              "ANZSRC_FOR_1": ["ANZSRC_FOR_1"],
              "ANZSRC_FOR_2": ["ANZSRC_FOR_2"],
              "ANZSRC_FOR_3": ["ANZSRC_FOR_3"]
            ]
            scripts = [
              'id': [scriptPath: 'scripts/dc_identifier.groovy']
            ]
          }
        }
      }
      logging {
        trace = true
      }
      api {
        key = '' // IMPORTANT: set the api key to match Mint's
        scheme = "http"
        host = "localhost"
        port = 9001
        path = 'mint/default/api/package/'
        actions {
          create = "create.script"
          attach = "attach.script"
          transform = "transform.script"
          ping = "info.script"
          find = "find.script"
        }
      }
    }
}

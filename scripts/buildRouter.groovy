/**
* Creates the Uploaders's  Camel REST routes using DSL. 
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/
import org.apache.camel.builder.*
import org.apache.camel.model.rest.*
import org.apache.camel.processor.interceptor.*
import org.apache.camel.*
import org.apache.camel.model.dataformat.*
import au.com.redboxresearchdata.harvester.redbox.util.*
  
// CSV Config
CsvDataFormat csv = new CsvDataFormat()
csv.skipHeaderRecord = true;
csv.useMaps = true;

// Utilities and API Wrapper
def parser = new Netty4HttpMultipartParser(config:config, log:log)
Class groovyClass = new GroovyClassLoader(routeBuilder.class.classLoader).parseClass(new File(config.baseDir + config.apiWrapper).text)
def apiWrapper = groovyClass.newInstance()
apiWrapper.config = config
apiWrapper.log = log

// REST Component config
routeBuilder.restConfiguration()
.component("netty4-http")
.host("localhost")
.port(config.port)
.bindingMode(RestBindingMode.off)
.componentProperty('traceEnabled', config.logging.trace.toString())
.componentProperty('mapHeaders', 'false')
.enableCORS(true)

// REST endpoints definition
routeBuilder.rest("/api/${config.version}/")
.post("/mint/csv").consumes("multipart/form-data").produces('application/json').to("direct:uploadfile")
.get('/mint/csv/ping').to('direct:ping')

// Channel Definitions
routeBuilder.from("direct:uploadfile").transform { exchange ->
  def parsedData = parser.parseAndSave(exchange)
  apiWrapper.fileUploaded(exchange, parsedData)
  parsedData.files[0].file.text
}
.choice()
  .when(routeBuilder.header('notParse').isNull())
    .to("direct:parseCsv")
  .otherwise()
    .to('direct:respond')

routeBuilder.from('direct:parseCsv')
.unmarshal(csv)
.split(routeBuilder.body())
.process { exchange ->
  log.debug '------------------- Processing csv ----------------------------'
  log.debug exchange.in.body.toString()
  apiWrapper.processCsv(exchange)
}
.end()
.transform {exchange ->
  def fileInfo = exchange.in.getHeader('file')
  parser.moveFile(config.upload.donedir, fileInfo.file, fileInfo.name)
}.to('direct:respond')

routeBuilder.from('direct:respond')
.transform {exchange->
  log.debug '------------------- Sending Reply ----------------------------'
  apiWrapper.prepareResponse(exchange)
}
.setBody().simple("\${header.resp}")
.removeHeader('resp')
.removeHeader('file')
.removeHeader('erroredRecIds')

routeBuilder.from('direct:ping').transform { exchange ->
  exchange.in.body = apiWrapper.ping()
}
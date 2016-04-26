package au.com.redboxresearchdata.harvester.redbox

import okhttp3.*
import java.io.*
import java.util.*
import groovy.util.*
import groovy.json.*
import java.security.*  
import groovy.json.*
/**
*
* Creates Mint Packages from CSV records. Provides domain specific implementation and validation of the packages.
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/  
class MintApiWrapper {
  def config
  def log 
  MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain");
  JsonSlurper slurper = new JsonSlurper()
  
  def ping() {
    Request request = new Request.Builder()
      .url(getUrl('ping').build())
      .header('apiKey', config.api.key)
      .build();
    def response = execRequest(request)
    def responseStr = response.body().string()
    response.body().close()
    return responseStr
  }
  
  def createRecord(data, packageType) {
    log.debug "Package Type: ${packageType}"
    if (!config.mapping[packageType]) {
      log.error "No mapping for package type: '${packageType}', please check your configuration."
      return false
    }
    def idColumn = config.mapping[packageType].idColumn
    def idVal = data[idColumn]
    log.debug "Using ID Column: ${idColumn}, with value: ${idVal}"
    def existingOids = existsPackage(config.mapping[packageType].idSearchField, idVal )
    def existingOid = null
    def create = null
    if (existingOids) {
      existingOid = existingOids[0]
    } else {
      create = createPackage(packageType)
      if (create.isSuccessful()) {
        def createJson = slurper.parseText(create.body().string())  
        create.body().close()  
        existingOid = createJson.oid
      }
    }
    if (existingOid) {
      // add attachments
      def attachments = config.mapping[packageType].attachments
      def attachTracker = [:]
      def allAttached = true
      attachments.each {att->
        def attConf = config.mapping[packageType][att]
        def template = new File(config.baseDir + attConf.template).text
        log.debug "Att: '${att}', Using template content:"
        log.debug template
        def attData = slurper.parseText(template)
        def attMapping = attConf.mapping
        if (attMapping) {
          mapFields(attMapping, attData, data)
        }
        def scripts = attConf.scripts
        if (scripts) {
          runScripts(scripts, attData, data)
        }
        def attach = attachPayload(existingOid, attConf.name, JsonOutput.toJson(attData))  
        attachTracker[att] = attach.isSuccessful()
        allAttached = allAttached && attach.isSuccessful()
      }
      if (!allAttached) {
        log.error "Some attachments failed, skipping record..."
        return false
      }
      transform(existingOid, packageType)
      return true
    }
    return false
  }
  
  def mapFields(mapping, targetData, srcData) {
    log.debug "Mapping fields..."
    mapping.each {srcField, targetFields ->
      targetFields.each {targetField ->
        log.debug "Setting '${targetField}' to '${srcData[srcField]}"
        targetData['data'][targetField] = srcData[srcField]
      }
    }
  }
  
  def runScripts(scripts, targetData, srcData) {
    log.debug "Running scripts..."
    GroovyShell shell = new GroovyShell()
    scripts.each {srcField, scriptConfig ->
      def scriptPath = config.baseDir + scriptConfig.scriptPath
      log.debug "Running '${scriptPath}' for '${srcField}'"
      def script = shell.parse(new File(scriptPath).text)
      script.setBinding(new Binding([config:config, srcField:srcField, targetData:targetData, data:srcData[srcField], srcData:srcData, scriptArgs:scriptConfig.args, log:log]))
      script.run()
    }
  }
  
  def existsPackage(searchFld, searchVal) {
    def searchRes = findPackage(searchFld, searchVal)
    return searchRes?.resultOids?.size() > 0 ? searchRes?.resultOids : false
  }
  
  def findPackage(searchFld, searchVal) {
    Request request = new Request.Builder()
      .url(getUrl('find', ['searchParam':searchFld, 'searchVal':searchVal]).build())
      .header('apiKey', config.api.key)
      .build();
    def res = execRequest(request)
    if (res.isSuccessful()) {
      def resJson = slurper.parseText(res.body().string())
      res.body().close()
      return resJson
    }
    return null
  }
  
  def createPackage(packageType) {
    Request request = new Request.Builder()
      .url(getUrl('create').addQueryParameter('packageType', packageType).build())
      .header('apiKey', config.api.key)
      .build();
    return execRequest(request)
  }
  
  def attachPayload(oid, payloadName, payloadContents) {
    RequestBody requestBody = new MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("oid", oid)
    .addFormDataPart("name", payloadName)
    .addFormDataPart("payload", payloadContents)
    .build()
    Request request = new Request.Builder()
      .url(getUrl('attach').build())
      .header('apiKey', config.api.key)
      .post(requestBody)
      .build()
    return execRequest(request)
  }
  
  def transform(oid, packageType) {
    Request request = new Request.Builder()
      .url(getUrl('transform').addQueryParameter('oid', oid).addQueryParameter('packageType', packageType).build())
      .header('apiKey', config.api.key) 
      .build();
    return execRequest(request)
  }
  
  def execRequest(request) {
    OkHttpClient client = new OkHttpClient.Builder()
    .addInterceptor(new LoggingInterceptor(log:log))
    .build()
    Response response = client.newCall(request).execute();
    if (!response.isSuccessful()) {
      log.error "Error in request: ${response.code}"
      log.error response.toString()
    } 
    return response
  }
  
  def getUrl(action, queryParams=null) {
    def builder = new HttpUrl.Builder()
    .scheme(config.api.scheme)
    .host(config.api.host)
    .port(config.api.port)
    def path = config.api.path + config.api.actions[action]
    for (segment in path.split('/')) {
      builder.addPathSegment(segment)
    }
    if (queryParams) {
      queryParams.each {k,v->
        builder.addQueryParameter(k,v)
      }
    }
    return builder
  }
  
  def generateMD5(String s) {
      MessageDigest digest = MessageDigest.getInstance("MD5")
      digest.update(s.bytes);
      new BigInteger(1, digest.digest()).toString(16).padLeft(32, '0')
   } 
  
  def fileUploaded(exchange, parsedData) {
    def packageType = parsedData.attributes['packageType']
    exchange.in.setHeader('file', parsedData.files[0])
    exchange.in.setHeader('packageType', packageType)
    exchange.in.setHeader('resp', [success:true, message:'Upload ok', erroredRecIds:[]])
    if (!packageType) {
      exchange.in.getHeader('resp').success = false
      exchange.in.getHeader('resp').message = 'No package type, skipping all records.'
      exchange.in.setHeader('notParse', 'nope')  
    }
    if (exchange.in.getHeader('resp').success == true) {
      if (!shouldParse(exchange, parsedData, packageType)) {
        exchange.in.setHeader('notParse', 'nope')   
        log.debug "Not parsing reason: ${exchange.in.getHeader('resp').message}"
      }
    }
  }
  
  def shouldParse(exchange, parsedData, packageType) {
    def fieldValidation = config.mapping[packageType]?.fieldValidation
    if (fieldValidation) {
      // check if the CSV columns exist
      parsedData.files[0].file.withReader {
        def line = it.readLine()
        def colHeaders = []
        def tempHdrs = line.split(',')
        tempHdrs.each {
          colHeaders <<  it.replace("\"", '').trim()
        }
        if (!colHeaders?.containsAll(fieldValidation)) {
          exchange.in.getHeader('resp').success = false
          exchange.in.getHeader('resp').message = 'The required columns not found on the file, please check if the package type matches your file format.' 
        }
      }  
    } else {
      exchange.in.getHeader('resp').success = false
      exchange.in.getHeader('resp').message = 'The package type is not configured on the server. Please contact support.'
    }
    return exchange.in.getHeader('resp').success 
  }
  
  def processCsv(exchange) {
    def packageType = exchange.in.getHeader('packageType')
    if (packageType) {
      if (!createRecord(exchange.in.body, packageType)) {
        exchange.in.getHeader('resp').success = false
        exchange.in.getHeader('resp').message = 'Some records failed to process.'
        exchange.in.getHeader('resp').erroredRecIds << "${exchange.in.body[config.mapping[packageType].idColumn]}"
      }  
    } else {
      log.error "No package type, skipping record."
    }
  }
  
  def prepareResponse(exchange) {
    if (!exchange.in.getHeader('resp').success) {
      exchange.in.setHeader(exchange.HTTP_RESPONSE_CODE, 523)  
    }
    exchange.in.setHeader('resp', JsonOutput.toJson(exchange.in.getHeader('resp')))
  }
}

class LoggingInterceptor implements Interceptor {
  def log

  public Response intercept(Interceptor.Chain chain) throws IOException {
    Request request = chain.request();

    long t1 = System.nanoTime();
    log.info(String.format("Sending request %s on %s%n%s",
        request.url(), chain.connection(), request.headers()));
    if (request.body()) {
      log.debug "Body contents:"
      if (request.body() instanceof MultipartBody) {
        for (part in request.body().parts()) {
          log.debug part.toString()
        } 
      } else {
        log.debug request.body().string()  
        request.body().close()
      }
    }

    Response response = chain.proceed(request);

    long t2 = System.nanoTime();
    log.info(String.format("Received response for %s in %.1fms%n%s",
        response.request().url(), (t2 - t1) / 1e6d, response.headers()));

    return response;
  }
}
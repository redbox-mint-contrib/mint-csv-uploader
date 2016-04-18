/***
*
* Generates a local identifier using the RecordIDPrefix
*
* Bindings:
*
* `config` - configuration
* `targetData` - the resulting map 
* `data` - value to add as another keyword
* `scriptArgs` - arguments
* `srcData` - the entire CSV row in a map
* `srcField` - the CSV column name
* `log` - logger
*
* Author: <a href='https://github.com/shilob'>Shilo Banihit</a>
*/

targetData.metadata['dc.identifier'] = targetData.recordIDPrefix + data
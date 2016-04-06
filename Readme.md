Mint CSV Uploader
===========================

#Purpose

This is a set of scripts that aims to provide a quick & simple API to allow csv import of data into Mint. 

#Architecture





                                      +----------------------------------------------------+
                                      |                        Firewall                    |
                                      |                                                    |
                                      |                         +-----------------------+  |
                                      |                         |     WAR or Fat Jar    |  |
                                      |                         | +-------------------+ |  |
    +------------------+             +----------+               | | Mint CSV Uploader | |  |
    |                  |             ||         +---------------> +-------------------+ |  |
    |  User's Browser  +--------------|  Mint   |               |                       |  |
    |                  |             ||         <---------------+ +-------------------+ |  |
    +------------------+             +----------+               | | ReDBox API Base   | |  |
                                      |                         | +-------------------+ |  |
                                      |                         |                       |  |
                                      |                         +-----------------------+  |
                                      |                                                    |
                                      +----------------------------------------------------+







The Mint provides a facade to the API, providing a consistent user interface and auth paths. The Uploader provides a quick way to implement domain and/or institution specific functionality and validation in an agile manner. The Uploader should only be accessible to Mint, as there's no inbuilt security for now.

#Endpoints
`GET /api/<version>/mint/csv/ping` - returns the status of the uploader, validating the integration to Mint
`POST /api/<version>/mint/csv/` - expects a Multipart form post, with one file and a 'packageType'

#Deployment

* Clone this repository and modify `api.key` value of `config.groovy` to match the API key set in Mint.
* Download a pre-built [RedBox API Base](https://github.com/redbox-mint-contrib/redbox-api-base) and deploy as you see fit, setting the `redboxApiConfig` environment variable to the `config.groovy` from the cloned repository.


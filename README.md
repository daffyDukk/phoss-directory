# peppol-yellow-pages
The official PEPPOL Yellow Pages (PYP) software. It is split into the following sub-projects:
  * `peppol-yellow-pages-api` - the common API for the indexer and the publisher incl. Lucene handling
  * `peppol-yellow-pages-indexer` - the PYP indexer part
  * `peppol-yellow-pages-publisher` - the PYP publisher web application
  
Status as per 2015-10-07: 
  * This project was started on 2015-08-31
  * The indexer part (incl. REST interface) looks quite good and only requires (currently) some documentation. Unit tests with concurrent indexing requests work flawlessly incl. Lucene based storage and lookup. The first real BusinessInformation lookup for `9915:test` also succeeded.
  * The publisher website was started and the search already works. The first screenshots are available on Google Drive.
  
Open tasks according to the design document:
  * The REST query API must be added
  * The extended search for the UI must be added
  * An administration GUI (e.g. auditing of index actions) would be nice
  * A Java library to be used in SMPs to communicate with the PYP must be added
  * A final logo must be decided upon :) 

# Building requirements
To build the PYP software you need at least Java 1.8 and Apache Maven 3.x. Configuration for usage with Eclipse 4.5 is contained in the repository.

Additionally to the contained projects you MAY need the latest SNAPSHOT of [ph-oton](https://github.com/phax/ph-oton) as part of your build environment. 

# PYP Indexer
The PYP indexer is a REST component that is responsible for taking indexing requests from SMPs and processes them in a queue (PEPPOL SMP client certificate required). Only the PEPPOL participant identifiers are taken and the PYP Indexer is responsible for querying the respective SMP data directly. Therefore the respective SMP must have the appropriate `Extension` element of the service group filled with the business information metadata as required by PYP. Please see the PYP specification draft on [Google Drive](https://drive.google.com/drive/folders/0B8Jct_iOJR9WfjJSS2dfdVdZYzBQMFotdmZoTXBZRl9Gd0cwdnB6cDZOQVlYbElrdEVVXzg)  for a detailed description of the required data format as well as for the REST interface.

# PYP Publisher
The PYP publisher is the publicly accessible web site with listing and search functionality for certain participants.

---

On Twitter: <a href="https://twitter.com/philiphelger">Follow @philiphelger</a>

<h3>Group Project</h3>
<a href="https://github.com/dwydm/FilmVault" target="_blank">FilmVault - Backend</a></p>
<p></p>As the group and project leader, I was responsible for scheduling team meetings and creating an optimal working schedule</p>
<p>In the project, my involvement included the DTO system, MapStruct implementation, DB data initialization, co-creating entities, REST endpoints, methods for communication with external APIs, search optimization, app requirements, and business logic.</p>

<h4>Other co-creators</h4>
<p><a href="https://github.com/k2esone" target="_blank">k2esone</a> - implementation of the security, JTW Auth, ngram search</p>
<p><a href="https://github.com/RadBia" target="_blank">RadBia</a> - DB logic, co-creating entieties and communication with external API</p>
<p><a href="https://github.com/MWolok" target="_blank">MWolok</a> - designing frontend part of the application <a href="https://github.com/dwydm/FilmValut-Client" target="_blank">FilmVault - Client</a></p>**

<h1>FilmVault - Backend</h2>
<p>FilmVault is a Java and React application that provides users with access to actual and up-to-date streaming services for requested movies and TV series. FV allows users to register, select desired VOD platforms, and add already-seen movies and series to their accounts. The application utilizes an external movie database API to fetch crucial movie details.</p>

<h2>Requirements</h2>

<h4>FilmVault - Client</h4>
<p>Clone following FilmVault Clinet</p>
https://github.com/dwydm/FilmValut-Client.git
<p>For further guidance, make sure to check it's README</p>


<h4>Database API</h4>
<p>FilmVault utilizes external movie database and requires valid API to work properly.</p>
<p>In src/main/resources/ create <b>application.properties</b> with value <b>media.api.key=</b></p>

<p>For your own API key, go to https://developer.themoviedb.org/docs and request one for free</p>

<h4>Swagger</h4>
<p>Additionally, for viewing more detailed documentation, we've implemented Swagger.<br>
To <b>application.properties</b> simply add <b>springdoc.swagger-ui.path=/swagger-ui.html</b></p>

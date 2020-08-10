# render at https://structurizr.com/dsl

workspace "XDCC Search" "Documentation of the Xdcc Search Application." {

    model {
        user = person "User" "A user of the xdcc search app."

        ifttt = softwaresystem "IFTTT" "Used for sending push notifications" "Existing System"
        ixirc = softwaresystem "ixirc.com" "DCC Search engine" "Existing System"

        ircnet = softwaresystem "IRC network(s)" "Allows xdcc search users to connect to IRC bots and request their DCC offerings." "Existing System"
        remoteBot = person "Remote bot" "Remote bot offering DCC packs (files)." "remote IRC Bot"

        enterprise "xdcc search" {
            xdccsearch = softwaresystem "XDCC Search application" "Allows users to search and download IRC DCC offerings." {
                fileSystem = container "File system" "Files downloaded are stored in the downloads folder." "File System" "File System"
                singlePageApplication = container "Single-Page Application" "Provides all of the search and download functionality to users via their web browser." "JavaScript and CycleJs" "Web Browser"
                xdccsearchApplication = container "XDCC Search API Application" "Provides search and download functionality via a JSON/HTTP and eventbus API." "Java and Vert.x" {
                    routerVerticle = component "HTTP Controller with dynamic routes" "Allows users to use the XDCC Search Application with HTTP calls and subscribe to the eventbus." "Vert.x Web: Rest Controller and Eventbus Bridge"
                    notificationVerticle = component "Notifications verticle" "send push notifications via ifttt." "Vert.x verticle"
                    botVerticle = component "IRC bot verticle" "connects to IRC networks, joins channels and sends DCC requests to remote bots about their offerings." "Vert.x verticle"
                    localBot = component "Local bot" "Local bot(s) managing IRC connection." "Kitteh IRC Client" "local IRC Bot"
                    searchVerticle = component "Search verticle" "requests search queries about offerings matching search string." "Vert.x verticle"
                    dccReceiverVerticle = component "DCC receiver verticle" "establishes socket connections to or from remote bots for data transer." "Vert.x verticle"
                    filenameResolverVerticle = component "Filename resolver verticle" "checks for duplicate filenames and creates filenames to be used for download." "Vert.x verticle"
                    stateVerticle = component "State verticle" "Aggregates all events to current application state, queryable via Rest." "Vert.x verticle"
                }
            }
        }

        # relationships between people and software systems
        user -> xdccsearch "Searches and downloads DCC offerings using"
        xdccsearch -> ifttt "Sends transfer completion push notifications using"
        xdccsearch -> ixirc "Searches DCC offerings using"
        xdccsearch -> ircnet "Connects to"
        remoteBot -> ircnet "Connects to"

        # relationships to/from containers
        user -> xdccsearchApplication "Visits / using" "HTTP"
        user -> singlePageApplication "Searches and starts transfers for DCC offerings using"
        xdccsearchApplication -> singlePageApplication "Delivers Single Page App to the user's web browser"

        # relationships to/from components
        singlePageApplication -> routerVerticle "Makes API calls to" "JSON/HTTP"
        routerVerticle -> singlePageApplication "Streams events to" "Websocket"

        notificationVerticle -> ifttt "Makes API calls to" "JSON/HTTPS"
        searchVerticle -> ixirc "Makes API calls to" "JSON/HTTP"

        botVerticle -> localBot "Starts" "function call"

        localBot -> ircnet "Connects to" "IRC"
        localBot -> remoteBot "Requests DCC file offering from" "IRC"
        remoteBot -> dccReceiverVerticle "transfers DCC file offering" "IRC"

        searchVerticle -> routerVerticle "register route at" "Eventbus" "routing"
        botVerticle -> routerVerticle "register route at" "Eventbus" "routing"
        stateVerticle -> routerVerticle "register route at" "Eventbus" "routing"

        routerVerticle -> searchVerticle "sends search request to" "Eventbus"
        routerVerticle -> botVerticle "starts irc bot via" "Eventbus"
        routerVerticle -> stateVerticle "requests current application state from" "Eventbus"

        filenameResolverVerticle -> fileSystem "create valid filename from" "function call"
        dccReceiverVerticle -> fileSystem "write files to" "function call"
        dccReceiverVerticle -> filenameResolverVerticle "request valid filename from" "Eventbus"
        dccReceiverVerticle -> routerVerticle "publishes file transfer progress" "Eventbus" "transfer progress"
        dccReceiverVerticle -> stateVerticle "publishes file transfer progress" "Eventbus" "transfer progress"
        dccReceiverVerticle -> botVerticle "publishes file transfer progress" "Eventbus" "transfer progress"
        dccReceiverVerticle -> notificationVerticle "publishes file transfer progress" "Eventbus" "transfer progress"

        localBot -> botVerticle "send bot operations" "function call"
        botVerticle -> stateVerticle "publishes bot operations" "Eventbus" "bot operations"
        botVerticle -> routerVerticle "publishes bot operations" "Eventbus" "bot operations"

    }

    views {
        systemcontext xdccsearch "SystemContext" {
            include *
            autoLayout
        }

        container xdccsearch "Containers" {
            include *
            animationStep user xdccsearchApplication
            animationStep singlePageApplication
            animationStep ixirc ircnet
        }

        component xdccsearchApplication "Components" {
            include *
            animationStep singlePageApplication
            animationStep routerVerticle searchVerticle ixirc
            animationStep botVerticle localBot remoteBot ircnet stateVerticle
            animationStep filenameResolverVerticle dccReceiverVerticle notificationVerticle
        }


        styles {
            element "Person" {
                background #08427b
                color #ffffff
                fontSize 22
                shape Person
            }
            element "local IRC Bot" {
                shape Robot
                background #85bbf0
                color #000000
            }
            element "remote IRC Bot" {
                shape Robot
                background #999999
                color #ffffff
            }
            element "Software System" {
                background #1168bd
                color #ffffff
            }
            element "Existing System" {
                background #999999
                color #ffffff
            }
            element "Container" {
                background #438dd5
                color #ffffff
            }
            element "Web Browser" {
                shape WebBrowser
            }
            element "File System" {
                shape Cylinder
            }
            element "Component" {
                background #85bbf0
                color #000000
            }

            relationship "Relationship" {
                thickness 3
                fontsize 25
            }
            relationship "routing" {
                color #f04040
            }
            relationship "bot operations" {
                color #4040f0
            }
            relationship "transfer progress" {
                color #40a040
            }
        }
    }
}
this is a new commit and the start of my chess project



SequenceDiagram.orgSams sequence diagrams [Not saved]   move work areanew diagramopen source textsave source textexport diagram
add participantzoom inzoom out
1
actor Client
2
participant Server
3
participant Handler
4
participant Service
5
participant DataAccess
6
database db
7
​
8
entryspacing 0.6
9
participant ChangeMe1
10
group#43829c #lightblue Registration
11
Client -> Server: [POST] /user\n{"username":" ", "password":" ", "email":" "}
12
Server -> Handler: {"username":" ", "password":" ", "email":" "}
13
Handler -> Service: register(RegisterRequest)
14
Service -> DataAccess: getUser(username)
15
DataAccess -> db:Find UserData by username
16
break User with username already exists
17
DataAccess --> Service: UserData
18
Service --> Server: AlreadyTakenException
19
Server --> Client: 403\n{"message": "Error: username already taken"}
20
end
21
DataAccess --> Service: null
22
Service -> DataAccess:createUser(userData)
23
DataAccess -> db:Add UserData
24
Service -> DataAccess:createAuth(authData)
25
DataAccess -> db:Add AuthData
26
Service --> Handler: RegisterResult
27
Handler --> Server: {"username" : " ", "authToken" : " "}
28
Server --> Client: 200\n{"username" : " ", "authToken" : " "}
29
end
30
​
31
group#orange #FCEDCA Login
32
Client -> Server: [POST] /session\n{ "username": "", "password": "" }
33
Server -> Handler: { "username": "", "password": "" }
34
Handler -> Service: login(LoginRequest)
35
Service -> DataAccess: getUser(username)
36
DataAccess -> db: Find UserData by username
37
break Not found OR invalid password
38
DataAccess --> Service: null
39
Service --> Handler: (error)
40
Handler --> Server: {"message": "Error: unauthorized"}
41
Server --> Client: 401 {"message": "Error: unauthorized"}
42
end
43
DataAccess --> Service: UserData
44
Service -> DataAccess: createAuth(authData)
45
DataAccess -> db: Add AuthData
46
Service --> Handler: LoginResult(username, authToken)
47
Handler --> Server: {"username": "", "authToken": ""}
48
Server --> Client: 200 {"username": "", "authToken": ""}
49
end
50
​
51
group#green #lightgreen Logout
52
Client -> Server: [DELETE] /session\nAuthorization: authToken
53
Server -> Handler: { authToken }
54
Handler -> Service: logout(LogoutRequest)
55
Service -> DataAccess: deleteAuth(authToken)
56
DataAccess -> db: Remove AuthData
57
Service --> Handler: LogoutResult
58
Handler --> Server: {}
59
Server --> Client: 200 {}
60
end
61
​
62
group#red #pink List Games
63
Client -> Server: [GET] /game\nAuthorization: authToken
64
Server -> Handler: { authToken }
65
Handler -> Service: listGames(ListGamesRequest)
66
Service -> DataAccess: getAuth(authToken)
67
DataAccess -> db: Find AuthData by token
68
break Invalid/expired token
69
DataAccess --> Service: null
70
Service --> Handler: (error)
71
Handler --> Server: {"message": "Error: unauthorized"}
72
Server --> Client: 401 {"message": "Error: unauthorized"}
73
end
74
DataAccess --> Service: AuthData
75
Service -> DataAccess: listGamesForUser(userID)
76
DataAccess -> db: Find games for user
77
DataAccess --> Service: [games]
78
Service --> Handler: ListGamesResult([games])
79
Handler --> Server: {"games": [ {gameID, whiteUsername, blackUsername, gameName} ] }
80
Server --> Client: 200 {"games": [...] }
81
end
82
​
83
group#d790e0 #E3CCE6 Create Game
84
Client -> Server: [POST] /game\nAuthorization: authToken\n{ "gameName": "" }
85
Server -> Handler: { authToken, gameName }
86
Handler -> Service: createGame(CreateGameRequest)
87
Service -> DataAccess: getAuth(authToken)
88
DataAccess -> db: Find AuthData by token
89
break Invalid/expired token

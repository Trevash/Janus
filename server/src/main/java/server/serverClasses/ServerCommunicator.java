package server.serverClasses;

import com.bignerdranch.android.shared.GenericCommand;
import com.bignerdranch.android.shared.Constants;
import com.bignerdranch.android.shared.requestObjects.CreateGameRequest;
import com.bignerdranch.android.shared.requestObjects.DrawTrainCardRequest;
import com.bignerdranch.android.shared.requestObjects.JoinGameRequest;
import com.bignerdranch.android.shared.requestObjects.StartGameRequest;
import com.bignerdranch.android.shared.resultobjects.ChatboxData;
import com.bignerdranch.android.shared.resultobjects.ClaimRouteData;
import com.bignerdranch.android.shared.resultobjects.DrawTrainCardData;
import com.bignerdranch.android.shared.resultobjects.GameListData;
import com.bignerdranch.android.shared.resultobjects.GameStatusData;
import com.bignerdranch.android.shared.resultobjects.Results;
import com.bignerdranch.android.shared.resultobjects.ReturnDestinationCardData;
import com.bignerdranch.android.shared.Serializer;
import com.bignerdranch.android.shared.models.*;
import com.sun.net.httpserver.HttpServer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ServerCommunicator extends WebSocketServer {
    private static final int MAX_WAITING_CONNECTIONS = 12;
    private HttpServer server;
    private Map<String, WebSocket> usernameWSMap = new HashMap<>();

    private ServerCommunicator(InetSocketAddress address) {
        super(address);
    }

    public static void main(String[] args) {
        //String host = "10.37.93.67";
        String host = Constants.IP_ADDRESS;
        int port = Constants.PORT;

        WebSocketServer server = new ServerCommunicator(new InetSocketAddress(host, port));
        server.run();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Server open!");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Server closed!");
        Iterator<String> iterator = usernameWSMap.keySet().iterator();
        while(iterator.hasNext()) {
        	String username = iterator.next();
        	if(usernameWSMap.get(username).equals(conn)) {
        		iterator.remove();
        	}
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        GenericCommand command = Serializer.getInstance().deserializeCommand(message);
        Results result = command.execute();
        String resultGson = Serializer.getInstance().serializeObject(result);

        switch (result.getType()) {
            case "Login":
                broadcastOne(resultGson, conn);
                if (result.isSuccess()) {
                    updateAllUserGameList();
                    userModel user = (userModel) result.getData(userModel.class);
                    usernameWSMap.put(user.getUserName().getValue(), conn);
                }
                break;
            case "Register":
                broadcastOne(resultGson, conn);
                if (result.isSuccess()) {
                    updateAllUserGameList();
                    userModel user = (userModel) result.getData(userModel.class);
                    usernameWSMap.put(user.getUserName().getValue(), conn);
                }
                break;
            case "Create":
                broadcastOne(resultGson, conn);
                updateAllUserGameList();
                break;
            case "Join":
                broadcast(resultGson);
                updateAllUserGameList();
                break;
            case "Start":
            	gameModel gameStart = (gameModel) result.getData(gameModel.class);
            	broadcastGame(resultGson, gameStart);
                broadcast(resultGson);
                updateAllUserGameList();
                break;
            case "UpdateChat":
                //TODO: Caleb change this later
            	ChatboxData chatboxData = (ChatboxData) result.getData(ChatboxData.class);
            	gameIDModel gameID = chatboxData.getGameID();
            	gameModel gameChat = serverModel.getInstance().getGameByID(gameID);
            	broadcastGame(resultGson, gameChat);
                break;
            case "ClaimRoute":
                ClaimRouteData claimRouteData = (ClaimRouteData) result.getData(ClaimRouteData.class);
                broadcastGame(resultGson, serverModel.getInstance().getGameByID(claimRouteData.getGameID()));
                updateGameStatus(claimRouteData.getGameID(), claimRouteData.getUsername(), "Route from " + claimRouteData.getCurRoute().getCity1().getName() + " to " + claimRouteData.getCurRoute().getCity2().getName() + " claimed by " + claimRouteData.getUsername().getValue());
                break;
            case "DrawDestinationCards":
            	broadcastOne(resultGson, conn);
            case "ReturnDestinationCards":
            	//gameModel game = (gameModel) result.getData(gameModel.class);
            	//broadcastGame(resultGson, game);
            	ReturnDestinationCardData returnDestdata = (ReturnDestinationCardData) result.getData(ReturnDestinationCardData.class);
                //broadcastGame(resultGson, serverModel.getInstance().getGameByID(returnDestdata.getGameID()));
                //broadcastOne(resultGson, conn);
            	updateGameStatus(returnDestdata.getGameID(), returnDestdata.getUsername(), "drew " + Integer.toString(returnDestdata.getSelectedCards().size()) + " destination cards");
            case "DrawFirstTrainCard":
                broadcastOne(resultGson, conn);
                break;
            case "DrawSecondTrainCard":
                DrawTrainCardData data = (DrawTrainCardData) result.getData(DrawTrainCardData.class);
                broadcastGame(resultGson, serverModel.getInstance().getGameByID(data.getGameID()));
                updateGameStatus(data.getGameID(), data.getUsername(), data.getUsername().getValue() + " drew train cards");
                break;
            case "ERROR":
                broadcastOne(resultGson, conn);
                break;
            default:
                System.out.println("Invalid type passed to onMessage from Result: " + result.getType());
        }
        // TODO what is this doing? It does make for a default-case
        List<WebSocket> temp = new ArrayList<>();
        temp.add(conn);
        broadcast(resultGson, temp);
    }
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println(ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
    }
    
    private void updateAllUserGameList() {
        Results gameListResult = new Results("GameList", true,
                new GameListData(serverModel.getInstance().getGames()));
        String gameListGson = Serializer.getInstance().serializeObject(gameListResult);
        broadcast(gameListGson);
    }

    public void broadcastOne(String resultGson, WebSocket conn) {
        List<WebSocket> temp = new ArrayList<>();
        temp.add(conn);
        broadcast(resultGson, temp);
    }

    public void broadcastGame(String resultGson, gameModel game) {
    	List<WebSocket> temp = new ArrayList<>();

    	for(playerModel player: game.getPlayers()) {
    		String username = player.getUserName().getValue();
    		if(usernameWSMap.containsKey(username)) {
    			temp.add(usernameWSMap.get(username));
    		}
    	}
    	
    	broadcast(resultGson, temp);
    }

    public void updateGameStatus(gameIDModel gameID, usernameModel username, String historyUpdate) {
        gameModel curGame = serverModel.getInstance().getGameByID(gameID);
        curGame.incrementTurnCounter();
        curGame.updateGameHistory(new chatMessageModel(username, historyUpdate));
        GameStatusData data = new GameStatusData(curGame.getTurnCounter(), curGame.getGameHistory(), curGame.getNumTrainCards(), curGame.getNumDestinationCards());
        Results result = new Results("UpdateGameStatus", true, Serializer.getInstance().serializeObject(data));
        this.broadcastGame(Serializer.getInstance().serializeObject(result), curGame);
    }
}

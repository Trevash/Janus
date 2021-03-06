package server.serverClasses;

import com.bignerdranch.android.shared.exceptions.CannotDrawTrainCardException;
import com.bignerdranch.android.shared.exceptions.InvalidAuthorizationException;
import com.bignerdranch.android.shared.exceptions.RouteNotFoundException;
import com.bignerdranch.android.shared.models.abstractRoute;
import com.bignerdranch.android.shared.models.authTokenModel;
import com.bignerdranch.android.shared.models.colors.cardColorEnum;
import com.bignerdranch.android.shared.models.colors.playerColorEnum;
import com.bignerdranch.android.shared.models.doubleRouteModelFew;
import com.bignerdranch.android.shared.models.doubleRouteModelMany;
import com.bignerdranch.android.shared.models.gameIDModel;
import com.bignerdranch.android.shared.models.gameModel;
import com.bignerdranch.android.shared.models.playerModel;
import com.bignerdranch.android.shared.models.singleRouteModel;
import com.bignerdranch.android.shared.models.trainCardModel;
import com.bignerdranch.android.shared.models.userModel;
import com.bignerdranch.android.shared.requestObjects.ClaimRouteRequest;
import com.bignerdranch.android.shared.requestObjects.DrawTrainCardRequest;
import com.bignerdranch.android.shared.requestObjects.JoinGameRequest;
import com.bignerdranch.android.shared.requestObjects.StartGameRequest;
import com.bignerdranch.android.shared.requestObjects.UpdateChatboxRequest;
import com.bignerdranch.android.shared.exceptions.DuplicateException;
import com.bignerdranch.android.shared.exceptions.GameNotFoundException;
import com.bignerdranch.android.shared.exceptions.UserNotFoundException;
import com.bignerdranch.android.shared.resultobjects.ChatboxData;
import com.bignerdranch.android.shared.resultobjects.ClaimRouteData;
import com.bignerdranch.android.shared.resultobjects.DrawTrainCardData;
import com.bignerdranch.android.shared.resultobjects.Results;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import server.plugIn.FactoryCreator;
import server.plugIn.IDaoFactory;
import server.plugIn.IGameDao;
import server.plugIn.IUserDao;

public class serverModel {
    private static serverModel sm = null;

    private List<userModel> users = new ArrayList<>();

    private List<gameModel> games = new ArrayList<>();

    //private String daoPlugIn = null;
    private IGameDao gameDao;
    private IUserDao userDao;
    private int deltas;

    public static serverModel getInstance() {
        if (sm == null) {
            sm = new serverModel();
        }
        return sm;
    }

    /**
     * @param plugInName plugInName must be the name of the IDaoFactory in server.plugIn -
     *                   note that server.plugIn is automatically added, so only the name of the
     *                   actual class is needed
     */
    public void setPlugIn(String plugInName) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        //daoPlugIn = plugInName;
        IDaoFactory factory = FactoryCreator.getFactory(plugInName);
        gameDao = factory.createGameDao();
        userDao = factory.createUserDao();
        // TODO change the server so that it actually uses the plugin
        //gameDao.addGame(null, null);
        //gameDao.retrieveGames();
    }

    public void setDeltas(int deltas) {
        this.deltas = deltas;
    }

    public IGameDao getGameDao() {
        return gameDao;
    }

    public IUserDao getUserDao() {
        return userDao;
    }

    public int getMaxDeltas() {
        return deltas;
    }

    public ClaimRouteData claimRoute(ClaimRouteRequest request) throws Exception {
        gameModel curGame = this.getGameByID(request.getGameID());
        abstractRoute curRoute = curGame.getRouteById(request.getRouteID());

        if (curRoute instanceof singleRouteModel) {
            curRoute.claim(request.getPlayerID());
        } else if (curRoute instanceof doubleRouteModelFew) {
            curRoute.claim(request.getPlayerID());
        } else if (curRoute instanceof doubleRouteModelMany) {
            curRoute.claim(request.getPlayerID(), request.getColor());
        } else {
            throw new RouteNotFoundException("Route of invalid class type passed to server!");
        }

        LinkedList<trainCardModel> discards = curGame.getPlayerModelFromID(request.getPlayerID()).addToClaimedRoutes(curRoute, request.getColor());
        curGame.addToTrainDiscards(discards);
        List<trainCardModel> hand = curGame.getPlayerModelFromID(request.getPlayerID()).getTrainCardHand();
        int points = curGame.getPlayerModelFromID(request.getPlayerID()).getPoints();
        // advances the turn/state
        curGame.onRouteClaimed(request.getPlayerID(), serverFacade.getInstance());

        return new ClaimRouteData(curGame.getGameID(), request.getPlayerID(), curGame.getRoutes(),
                hand, curGame.getTrainCardDiscards(), curRoute, points,
                getUser(request.getAuth()).getUserName(),
                curGame.getPlayerModelFromID(request.getPlayerID()).getLocomotives());
    }

    public void addUser(userModel newUser) {
        this.users.add(newUser);
    }

    public boolean userExists(String test) {
        for (userModel user : this.users) {
            if (test.equals(user.getUserName().getValue())) {
                return true;
            }
        }
        return false;
    }

    public userModel getUser(String username) throws UserNotFoundException {
        for (userModel user : this.users) {
            if (user.getUserName().getValue().equals(username)) {
                return user;
            }
        }
        throw new UserNotFoundException("User not found!");
    }

    public userModel getUser(authTokenModel auth) throws UserNotFoundException {
        for (userModel user : this.users) {
            if (user.getAuthToken().getValue().equals(auth.getValue())) {
                return user;
            }
        }
        throw new UserNotFoundException("User not found!");
    }

    public void setUsers(List<userModel> users){this.users = users;}

    public boolean authTokenExists(authTokenModel auth) {
        return true;
//        for (userModel curUser : this.users) {
//            if (curUser.getAuthToken().getValue().equals(auth.getValue()))
//                return true;
//        }
//        return false;
    }

    public List<gameModel> getGames() {
        return games;
    }

    public void setGames(List<gameModel> games){this.games = games;}

    public gameModel getGameByID(gameIDModel id) {
        for (gameModel game : this.games) {
            if (id.equals(game.getGameID())) {
                return game;
            }
        }
        return null;
    }

    public void addGame(gameModel newGame) {
        games.add(newGame);
    }

    public gameModel joinGame(JoinGameRequest request) throws GameNotFoundException,
            InvalidAuthorizationException {
        for (gameModel curGame : this.games) {
            if (curGame.getGameID().getValue().equals(request.getGameID().getValue())) {
                curGame.addPlayer(this.makeNewPlayer(this.getUserByAuth(request.getAuth())));
                return curGame;
            }
        }
        throw new GameNotFoundException("Join game failed, game not found");
    }

    private playerModel makeNewPlayer(userModel userByAuth) {
        // player color gets changed based on their position in the game - so actually unneeded
        return new playerModel(userByAuth.getUserName(), false, false, playerColorEnum.BLUE);
    }

    private userModel getUserByAuth(authTokenModel auth) throws InvalidAuthorizationException {
        for (userModel curUser : this.users) {
            if (curUser.getAuthToken().getValue().equals(auth.getValue()))
                return curUser;
        }
        throw new InvalidAuthorizationException("User not found by auth token to find game!");
    }

    public gameModel startGame(StartGameRequest request) throws GameNotFoundException {
        for (gameModel curGame : this.games) {
            if (curGame.getGameID().equals(request.getGameID())) {
                curGame.startGame();
                return curGame;
            }
        }
        throw new GameNotFoundException("Game not found to start!");
    }

    public ChatboxData updateChatbox(UpdateChatboxRequest request) throws GameNotFoundException {
        for (gameModel curGame : this.games) {
            if (curGame.getGameID().getValue().equals(request.getGameID().getValue())) {
                curGame.updateChatbox(request.getMessage());
                return new ChatboxData(curGame.getChatbox(), curGame.getGameID());
            }
        }
        throw new GameNotFoundException("Update chat failed, game not found");
    }


    public Results drawFirstTrainCard(DrawTrainCardRequest request) throws Exception {
        if (!serverModel.getInstance().authTokenExists(request.getAuthtoken())) {
            throw new InvalidAuthorizationException("Invalid Auth Token passed to drawFirstTrainCard");
        }

        gameModel curGame = serverModel.getInstance().getGameByID(request.getGameID());
        playerModel curPlayer = curGame.getPlayerModelFromID(request.getPlayerID());
        if (request.getIndex() == 0) {
            curPlayer.addTrainCardToHand(curGame.drawTrainCardFromDeck());
            return new Results("DrawFirstTrainCard", true,
                    new DrawTrainCardData(curGame.getGameID(), curPlayer.getTrainCardHand(),
                            curGame.getFaceUpCards(), curGame.getNumTrainCards(),
                            serverModel.getInstance().getUser(request.getAuthtoken()).getUserName()));
        } else {
            trainCardModel returnCard = curGame.drawFaceUpTrainCard(request.getIndex() - 1);
            if (returnCard.getColor() == cardColorEnum.LOCOMOTIVE) {
                curPlayer.addTrainCardToHand(returnCard);
                curGame.incrementTurnCounter(); // turn is over due to drawing a locomotive
                return new Results("DrawSecondTrainCard", true,
                        new DrawTrainCardData(curGame.getGameID(), curPlayer.getTrainCardHand(),
                                curGame.getFaceUpCards(), curGame.getNumTrainCards(),
                                serverModel.getInstance().getUser(request.getAuthtoken()).getUserName()));
            } else {
                curPlayer.addTrainCardToHand(returnCard);
                return new Results("DrawFirstTrainCard", true, new DrawTrainCardData(curGame.getGameID(), curPlayer.getTrainCardHand(), curGame.getFaceUpCards(), curGame.getNumTrainCards(), serverModel.getInstance().getUser(request.getAuthtoken()).getUserName()));
            }
        }
    }

    public Results drawSecondTrainCard(DrawTrainCardRequest request) throws Exception {
        if (!serverModel.getInstance().authTokenExists(request.getAuthtoken())) {
            throw new InvalidAuthorizationException("Invalid Auth Token passed to drawSecondTrainCard");
        }

        gameModel curGame = serverModel.getInstance().getGameByID(request.getGameID());
        playerModel curPlayer = curGame.getPlayerModelFromID(request.getPlayerID());
        if (request.getIndex() == 0) {
            curPlayer.addTrainCardToHand(curGame.drawTrainCardFromDeck());
            curGame.incrementTurnCounter();
            Results results = new Results("DrawSecondTrainCard", true,
                    new DrawTrainCardData(curGame.getGameID(), curPlayer.getTrainCardHand(),
                            curGame.getFaceUpCards(), curGame.getNumTrainCards(),
                            serverModel.getInstance().getUser(request.getAuthtoken()).getUserName()));
            return results;
        } else {
            trainCardModel returnCard = curGame.drawFaceUpTrainCard(request.getIndex() - 1);
            if (returnCard.getColor().name().equals(cardColorEnum.LOCOMOTIVE.name())) {
                throw new CannotDrawTrainCardException("Your second train card cannot be a faceup locomotive!");
            } else {
                curPlayer.addTrainCardToHand(returnCard);
                curGame.incrementTurnCounter();
                Results results = new Results("DrawSecondTrainCard", true,
                        new DrawTrainCardData(curGame.getGameID(), curPlayer.getTrainCardHand(),
                                curGame.getFaceUpCards(), curGame.getNumTrainCards(),
                                serverModel.getInstance().getUser(request.getAuthtoken()).getUserName()));
                return results;

            }
        }
    }
}

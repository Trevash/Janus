package com.janus.UI;

import android.content.pm.ActivityInfo;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.bignerdranch.android.shared.exceptions.DuplicateException;
import com.bignerdranch.android.shared.exceptions.RouteAlreadyClaimedException;
import com.bignerdranch.android.shared.models.abstractRoute;
import com.bignerdranch.android.shared.Constants;
import com.bignerdranch.android.shared.models.DestinationCardModel;
import com.bignerdranch.android.shared.models.chatMessageModel;
import com.bignerdranch.android.shared.models.gameModel;
import com.bignerdranch.android.shared.models.playerModel;
import com.bignerdranch.android.shared.models.usernameModel;
import com.bignerdranch.android.shared.models.colors.playerColorEnum;
import com.janus.ClientFacade;
import com.janus.Communication.WaitTask;
import com.janus.R;

import java.util.List;

public class GameActivity extends AppCompatActivity
        implements MapFragment.Context, RouteFragment.Context, DeckFragment.Context,
        DestinationSelectFragment.Context, StatusFragment.Context, WaitTask.Caller {
    private FragmentManager fm = getSupportFragmentManager();
    private int demoState = 0;
    private WaitTask task;

    // TODO figure out bug: sometimes a back button press returns player to game activity


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Fragment fragment = fm.findFragmentById(R.id.game_layout);

        if(fragment == null) {
            MapFragment mapFragment = new MapFragment();
            fm.beginTransaction()
                    .add(R.id.game_layout, mapFragment)
                    .commit();
            /*DestinationSelectFragment destinationFragment = new DestinationSelectFragment();
            fm.beginTransaction()
                    .add(R.id.game_layout, mapFragment)
                    .commit();*/
        }

        makeToast("You started the game!");
    }

    public void onClickDrawCard() {
        DeckFragment deckFragment = new DeckFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, deckFragment)
                .commit();
    }

    public void onFinishAction() {
        MapFragment gameFragment = new MapFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, gameFragment)
                .commit();
    }

    public void onClickClaimRoute() {
        RouteFragment routeFragment = new RouteFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, routeFragment)
                .commit();
    }

    public void onClickDestinationSelect() {
        DestinationSelectFragment destinationFragment = new DestinationSelectFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, destinationFragment)
                .commit();
    }

    public void onClickGameStatus() {
        StatusFragment statusFragment = new StatusFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, statusFragment)
                .commit();
    }

    public void onMapFragmentSelected() {
        MapFragment mapFragment = new MapFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, mapFragment)
                .commit();
    }

    public void onFinishWait(Integer demoState) {
        this.demoState = demoState;
        gameModel curGame = ClientFacade.getInstance().getGame();
        playerModel curPlayer = curGame.getHostPlayer();
        switch (demoState) {
            case 1:
                //● Add train cards for this player
                //● Update the number of invisible (face down) cards in train card deck and the visible (face up) cards in the train card deck
                makeToast("Drawing train cards, updating deck size and face-up trains");
                showDeckFragment();
                curPlayer.addTrainCardToHand(curGame.drawTrainCardFromDeck());
                try {
                    curPlayer.addTrainCardToHand(curGame.drawFaceUpTrainCard(0));
                    curPlayer.addTrainCardToHand(curGame.drawFaceUpTrainCard(1));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 2:
                //● Update the number of train cards for opponent players
                makeToast("Updated number of train cards for opponents");
                showStatusFragment();
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 3:
                //● Add claimed route (for any player). Show this on the map.
                makeToast("Claiming a route");
                showRouteFragment();
                List<abstractRoute> curRoutes = curGame.getRoutes();
                try {
                    curRoutes.get(0).claim(curPlayer.getId());
                } catch (RouteAlreadyClaimedException e) {
                    e.printStackTrace();
                }
                curPlayer.addToClaimedRoutes(curRoutes.get(0));
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 4:
                //● Show this on the map.
                makeToast("Showing claimed route on map");
                showMapFragment();
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 5:
                //● Update player points, the number of train cars, and cards for opponent players
                makeToast("Updated score, number of train cards and train cars for opponents");
                showStatusFragment();
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 6:
                //● Add player destination cards for this player
                makeToast("Claiming Destination Cards");
                showDestinationRoutesFragment();
                DestinationCardModel card1 = Constants.DestinationCards.ATLANTA_MONTREAL;
                DestinationCardModel card2 = Constants.DestinationCards.ATLANTA_NEW_YORK;
                curPlayer.DEMO_addDestinationCardToHand(card1);
                curPlayer.DEMO_addDestinationCardToHand(card2);
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 7:
                //● Update the number of cards in destination card deck
                makeToast("Updated number of destination cards for opponents");
                showDestinationRoutesFragment();
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 8:
                makeToast("Removing Destination cards from player");
                showDestinationRoutesFragment();
                curPlayer.DEMO_removeDestinationCardToHand(0);
                curPlayer.DEMO_removeDestinationCardToHand(0);
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 9:
                makeToast("Updated number of Destination cards for opponents");
                showDestinationRoutesFragment();
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 10:
                makeToast("Sending Chat message");
                showChatFragment();
                chatMessageModel message = new chatMessageModel(curPlayer.getUserName(), "Hey! I'm a ghost that hacked your chat function!");
                curGame.updateChatbox(message);
                task = new WaitTask(this);
                task.execute(demoState);
                break;
            case 11:
                makeToast("Incrementing turn order to iWillLose");
                usernameModel username = null;
                try {
                    username = new usernameModel("iWillLose");
                } catch (DuplicateException e) {
                    e.printStackTrace();
                }
                playerModel fakePlayer = new playerModel(username, true,true,playerColorEnum.YELLOW);

                //TODO: increment turn order
                task = new WaitTask(this);
                task.execute(demoState);
            default:
                makeToast("Demo done!");
                break;
        }
    }

    public void showDestinationRoutesFragment() {
    	StatusFragment fragment = new StatusFragment();
    	fragment.setWhichFragmentToShow(1);
        fm.beginTransaction()
        .replace(R.id.game_layout, fragment)
        .commit();
    }

    public void showDeckFragment() {
        DeckFragment fragment = new DeckFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, fragment)
                .commit();
    }

    public void showRouteFragment() {
        RouteFragment fragment = new RouteFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, fragment)
                .commit();
    }

    public void showMapFragment() {
        MapFragment fragment = new MapFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, fragment)
                .commit();
    }

    private void showStatusFragment() {
        StatusFragment fragment = new StatusFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, fragment)
                .commit();
    }

    private void showChatFragment() {
        ChatFragment fragment = new ChatFragment();
        fm.beginTransaction()
                .replace(R.id.game_layout, fragment)
                .commit();
    }

    public void onClickRunDemo() {
        //Demonstrate the following with pauses so the human eyes can read toasts and follow along
        makeToast("Here's the Starting Status");
        showStatusFragment();
        task = new WaitTask(this);
        task.execute(0);
    }



    private void makeToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}

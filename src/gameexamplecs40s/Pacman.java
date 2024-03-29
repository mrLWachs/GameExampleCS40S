
/** required package class namespace */
package gameexamplecs40s;

/** required imports */
import collections.LinkedList;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import tools.Animation;
import tools.Directions;
import tools.FileHandler;
import tools.GameCharacter;
import tools.MediaPlayer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;


/**
 * Pacman.java - representation of Pacman  
 *
 * @author Mr. Wachs 
 * @since 15-May-2019 
 */
public class Pacman extends GameCharacter
{
    
    private Prize                prize;
    private LinkedList<Wall>     walls;
    private LinkedList<Dot>      dots;
    private LinkedList<Ghost>    ghosts;  
    private LinkedList<Portal>   portals;
    private LinkedList<PowerDot> powerDots;
    private MediaPlayer          player;
    private FileHandler          file;
    private Timer                powerTimer;
    private boolean              inPowerMode;
    
    /** the total points pacman has received in the game */
    public int points;
    
    private final int ANIMATE_UP    = 0;
    private final int ANIMATE_DOWN  = 1;
    private final int ANIMATE_LEFT  = 2;
    private final int ANIMATE_RIGHT = 3;    
    private final int ANIMATE_DEATH = 4;    
    private final int RIGHT_PORTAL  = 0;
    private final int LEFT_PORTAL   = 1;
    
    
    /**
     * Constructor for the class, sets class property data
     * 
     * @param pacmanLabel the label associated with the image for the character
     * @param ghosts the list of wall objects
     * @param prize the prize object
     * @param walls the list of wall objects
     * @param dots the list of dot objects
     * @param portals the list of portal objects
     * @param powerDots the list of power dot objects
     * @param player the media player object
     * @param file the file handler object
     * @param settings
     */
    public Pacman(
            JLabel pacmanLabel, 
            LinkedList<Ghost> ghosts,
            Prize prize,
            LinkedList<Wall> walls, 
            LinkedList<Dot> dots,
            LinkedList<Portal> portals,
            LinkedList<PowerDot> powerDots,
            MediaPlayer player, 
            FileHandler file,
            LinkedList<String> settings) {
        super(pacmanLabel, Constants.PACMAN_MOVE_AMOUNT, Directions.STOP, 
                Constants.PACMAN_TIMER_DELAY, Directions.FOUR_DIRECTIONS);
        this.walls     = walls;             // associate parameters with objects
        this.ghosts    = ghosts;
        this.prize     = prize;
        this.dots      = dots;
        this.portals   = portals;
        this.powerDots = powerDots;
        this.player    = player;
        this.file      = file;
        inPowerMode    = false;
        setAnimations(pacmanLabel,settings);// sets all the animations
        setTimer();                         // set up a timer
        spawn();                            // spawn this pacman
    }

    /** The action this pacman does in it's timer */
    @Override
    public void action() {
        animate();                              // change animation to respond
        mover.move();                                   // move coordinates         
        for (int i = 0; i < walls.size(); i++) {        // traverse walls
            if (detector.isOverLapping(walls.get(i))) { // colliding with wall
                reactor.stickTo(walls.get(i));          // stick to wall
                i = walls.size();                       // exit loop early
                mover.stop();                           // stop movement
                animate();                              // change animation
            }
        }
        for (int i = 0; i < dots.size(); i++) {         // traverse dots
            if (detector.isOverLapping(dots.get(i))) {  // hit a dot
                dots.get(i).despawn();                  // remove dot
                player.playWAV(Constants.EAT_DOT_SOUND);// play sound
                points += Constants.DOT_POINT;          // add a point
            }
        }
        for (int i = 0; i < powerDots.size(); i++) {         // traverse powers
            if (detector.isOverLapping(powerDots.get(i))) {  // hit a power dot
                setPowerMode(true);
                powerDots.get(i).despawn();                  // remove power dot
                player.playWAV(Constants.EAT_DOT_SOUND);     // play sound
                points += Constants.POWER_DOT_POINT;         // add power point
            }
        }
        if (detector.isOverLapping(portals.get(RIGHT_PORTAL))) {    // hit portal
            reactor.stickToRight(portals.get(LEFT_PORTAL));         // teleport
        }
        if (detector.isOverLapping(portals.get(LEFT_PORTAL))) {     // hit portal
            reactor.stickToLeft(portals.get(RIGHT_PORTAL));         // teleport
        }        
        if (detector.isOverLapping(prize)) {            // hit prize
            prize.despawn();                            // remove prize
            player.playWAV(Constants.GAME_OVER_WIN_SOUND);    // play sound
            points += Constants.PRIZE_POINT;            // add a point
        }
        for (int i = 0; i < ghosts.size(); i++) {
            if (detector.isOverLapping(ghosts.get(i))) {
                if(inPowerMode) ghosts.get(i).despawn();
                else            loseGame();
            }
        }
        if (points >= 
            (Constants.DOT_POINT * dots.size()) + 
            Constants.PRIZE_POINT +
            (Constants.POWER_DOT_POINT * powerDots.size())) { // check end game
            winGame();                                  // win game
        }
        redraw();                                       // re draw
    }

    private void setPowerMode(boolean mode) {
        inPowerMode = mode;
        for (int i = 0; i < ghosts.size(); i++) {
            ghosts.get(i).isVulnerable = mode;
            ghosts.get(i).animate();
        }
        if (inPowerMode) powerTimer.start();
        else             powerTimer.stop();
    }
    
    /** Initiates the timer for when pacman eats a power dot */
    private void setTimer() {
        powerTimer = new Timer(Constants.POWER_TIMER_DELAY, 
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setPowerMode(false);
            }
        });
    }
    
    /** Pacman has won the game (eaten all the dots) */
    private void winGame() {
        for (int i = 0; i < ghosts.size(); i++) {   // traverse ghosts
            ghosts.get(i).mover.stop();             // stop all ghosts
        }
        sprite.animate(ANIMATE_DEATH);              // run animation
        player.playWAV(Constants.GAME_OVER_WIN_SOUND);  // play sound
        String name = JOptionPane.showInputDialog(
                "You win!\nEnter name");                // get user's name
        LinkedList<String> data = new LinkedList<>();   // create list
        data.add(name);                                 // add values to list
        data.add("" + points);
        file.write(data);                           // write array to file
        System.exit(0);                             // terminate application
    }

    /** Pacman has lost the game (captured by a ghost) */
    private void loseGame() {
        mover.stop();                                   // stop pacman
        sprite.animate(ANIMATE_DEATH);              // new pacman animation
        for (int i = 0; i < ghosts.size(); i++) {       // traverse ghosts
            ghosts.get(i).mover.stop();                 // stop all ghosts
        }
        player.playWAV(Constants.GAME_OVER_LOSE_SOUND); // play sound
        String name = JOptionPane.showInputDialog("Enter name"); // get name
        LinkedList<String> data = new LinkedList<>();   // create list
        data.add(name);                                 // add values to list
        data.add("" + points);
        file.write(data);                               // save array to file
        System.exit(0);                                 // exit application
    }
    
    /** Changes animation set to appropriate animation based on direction */
    private void animate() {
        if (sprite == null) return;
        if (sprite.hasAnimations() == false) return;
        if (coordinates.direction == Directions.UP) {
            if (sprite.isRunning(ANIMATE_UP) == false) 
                sprite.animate(ANIMATE_UP);
        }
        else if (coordinates.direction == Directions.DOWN) {
            if (sprite.isRunning(ANIMATE_DOWN) == false) 
                sprite.animate(ANIMATE_DOWN);
        }
        else if (coordinates.direction == Directions.LEFT) {
            if (sprite.isRunning(ANIMATE_LEFT) == false) 
                sprite.animate(ANIMATE_LEFT);
        }
        else if (coordinates.direction == Directions.RIGHT) {
            if (sprite.isRunning(ANIMATE_RIGHT) == false) 
                sprite.animate(ANIMATE_RIGHT);
        }
    }

    /**
     * Set up all the animations for this character
     * 
     * @param label the label to associate the animation with
     * @param settings the list of animation settings
     */
    private void setAnimations(JLabel label, LinkedList<String> settings) {
        String sheet = Constants.SPRITE_SHEET;
        int    delay = Constants.PACMAN_ANIMATION_DELAY; 
        String tag   = Constants.PACMAN_LEFT_TAG;        
        Animation pacmanLeft = Animator.getAnimation(sheet, label, 
                                                       delay, settings, tag);
        tag = Constants.PACMAN_RIGHT_TAG;        
        Animation pacmanRight = Animator.getAnimation(sheet, label, 
                                                       delay, settings, tag);
        tag = Constants.PACMAN_UP_TAG;        
        Animation pacmanUp = Animator.getAnimation(sheet, label, 
                                                       delay, settings, tag);
        tag = Constants.PACMAN_DOWN_TAG;        
        Animation pacmanDown = Animator.getAnimation(sheet, label, 
                                                       delay, settings, tag);
        tag = Constants.PACMAN_DEATH_TAG;        
        Animation pacmanDeath = Animator.getAnimation(sheet, label, 
                                                       delay, settings, tag);
        LinkedList<Animation> pacmanAnimations = new LinkedList<>(); 
        pacmanAnimations.add(pacmanUp);
        pacmanAnimations.add(pacmanDown);
        pacmanAnimations.add(pacmanLeft);
        pacmanAnimations.add(pacmanRight);
        pacmanAnimations.add(pacmanDeath);                
        pacmanAnimations.get(ANIMATE_DEATH).setDelay(
                Constants.PACMAN_DEATH_ANIMATION_DELAY);
        pacmanAnimations.get(ANIMATE_DEATH).setLoop(false);        
        sprite.setAnimations(pacmanAnimations);
    }

}

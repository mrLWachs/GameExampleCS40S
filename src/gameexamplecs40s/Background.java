
/** required package class namespace */
package gameexamplecs40s;

/** required imports */
import tools.GameObject;
import javax.swing.JLabel;


/**
 * Background.java - representation of a background in a game 
 *
 * @author Mr. Wachs 
 * @since 15-May-2019 
 */
public class Background extends GameObject
{

    /**
     * Constructor for the class, sets class property data
     * 
     * @param label the label associated with the image for the game object
     */
    public Background(JLabel label) {
        super(label);
    }
    
}

package np.com.sudishrestha.nkgames;


import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use the custom GameView instead of a layout file
        GameView gameView = new GameView(this);
        setContentView(gameView);
    }
}
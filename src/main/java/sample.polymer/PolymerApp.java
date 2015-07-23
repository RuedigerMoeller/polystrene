package sample.polymer;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.util.Log;

import java.util.HashMap;

/**
 * Created by ruedi on 12/07/15.
 */
public class PolymerApp extends Actor<PolymerApp> {

    public static final int CLIENT_QSIZE = 1000;

    Scheduler clientThreads[] = {
        new SimpleScheduler(CLIENT_QSIZE,true) // only one session processor thread should be sufficient for most apps.
    };

    String buzzWords = "";
    HashMap<PolymerUserSession,Callback> wordSubscriptions = new HashMap<>();

    public IPromise<PolymerUserSession> login( String user, String pwd ) {
        Promise result = new Promise<>();
        if ( "admin".equals(user) ) {
            // deny access for admin's
            result.reject("Access denied");
        } else {
            // create new session and assign it a random scheduler (~thread). Note that with async nonblocking style
            // one thread will be sufficient most of the time. For very computing intensive apps increase clientThreads to like 2-4
            PolymerUserSession sess = AsActor(PolymerUserSession.class,clientThreads[((int) (Math.random() * clientThreads.length))]);
            sess.setThrowExWhenBlocked(true);
            sess.init( self(), user );
            result.resolve(sess);
        }
        return result;
    }

    @Local
    public void clientClosed(PolymerUserSession session) {
        System.out.println("client closed "+session.getUserName().await());
        wordSubscriptions.remove(session);
    }

    @Local
    public void subscribe(PolymerUserSession session, Callback<String> cb) {
        wordSubscriptions.put(session, cb);
    }

    void fireEvent() {
        wordSubscriptions.values().forEach( cb -> {
            try {
                cb.stream(buzzWords);
            } catch (Throwable th) {
                Log.Info(null,th);
            }
        });
    }

    @Local
    public void addText(String text) {
        while( buzzWords.length() > 1024 ) {
            int idx = buzzWords.indexOf(" ");
            if ( idx <= 0 ) {
                buzzWords = "";
            } else {
                buzzWords = buzzWords.substring(idx);
            }
        }
        buzzWords += " "+text;
        fireEvent();
    }
}

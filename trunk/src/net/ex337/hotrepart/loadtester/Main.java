package net.ex337.hotrepart.loadtester;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.ex337.hotrepart.loadtester.dao.ItemDAO;
import net.ex337.hotrepart.loadtester.dao.ItemDAOMockImpl;
import net.ex337.hotrepart.loadtester.dao.ItemDAOPostgresImpl;
import net.ex337.hotrepart.loadtester.telemetry.GeneratedLoad;
import net.ex337.hotrepart.loadtester.telemetry.LoadReceiver;
import org.postgresql.ds.PGPoolingDataSource;

/**
 *
 * The main class for the load tester.
 *
 * Reads in the configuration, and executes the desired operation.
 *
 * @author ian
 */
public class Main implements LoadReceiver, Runnable {
    private static final String DEFAULT_CONFIG = "loadtester.properties";

    private static final List<String> VALID_OPS = new ArrayList<String>(){{
        add("loadtest");
        add("verify");
        add("mock");
    }};

    private static boolean validateArgs(String[] args) {

        if(args.length == 0 || args.length > 2) {
            return false;
        }

        if( ! VALID_OPS.contains(args[0].toLowerCase())) {
            return false;
        }

        if(args.length == 2) {
            File f = new File(args[1]);
            if( ! f.exists()) {
                System.out.println("File not found: "+f.getName());
                return false;
            }
        }

        return true;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main m = new Main(args);
        m.run();
    }

    private String[] args;
    /*
     * this is not thread-safe, but the method where this is used
     * is only called from one thread.
     */
    private DateFormat df = new SimpleDateFormat("hh:mm:ss");

    private Main(String[] args) {
        this.args = args;
    }

    public void run() {
        if( ! validateArgs(args)) {
            System.out.println("Usage:\n java -jar loadtester.jar mock|loadtest|verify [config file]");
            return;
        }

        LoadTesterConfiguration c;

        if(args.length == 2) {
            c = new LoadTesterConfiguration(new File(args[1]));
        } else {
            c = new LoadTesterConfiguration(new File(DEFAULT_CONFIG));
        }

        ItemDAO itemDAO = null;

        if("mock".equals(args[0].toLowerCase())) {
            
            itemDAO = new ItemDAOMockImpl();

        } else {
            PGPoolingDataSource ds = new PGPoolingDataSource();
            ds.setDataSourceName("ds");
            ds.setDatabaseName(c.getProxyDb());
            ds.setInitialConnections(c.getProxyPoolInitSize());
            ds.setMaxConnections(c.getProxyPoolMaxSize());
            ds.setPassword(c.getProxyPassword());
            ds.setPortNumber(c.getProxyPort());
            ds.setServerName(c.getProxyHost());
            ds.setUser(c.getProxyUser());

            itemDAO = new ItemDAOPostgresImpl(ds);
        }

        if("verify".equals(args[0].toLowerCase())) {

            DbVerifier v = new DbVerifier(itemDAO);
            v.run();

        } else {
            
            System.out.println("type\tperiod\t#ops\tsize(b)\ttime(ms)");

            LoadTestRunner runner = new LoadTestRunner(itemDAO, c, this);
            runner.run();
        }        

    }

    /*
     * Below is callbacks for the load & exception
     * information generated by the load tester.
     */

    public void addThrowable(Throwable e) {
        System.err.println(e.getMessage());
        e.printStackTrace(System.err);
    }

    public void addThrowables(List<Throwable> e) {
        for(Throwable t : e) {
            System.err.println(t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    public void addGeneratedLoad(GeneratedLoad l) {
        System.out.println(l.getCategory()+"\t"+df.format(new Date(l.getTime()))+"\t"+l.getNumOps()+"\t"+l.getOpsSize()+"\t"+l.getDuration());
    }


}

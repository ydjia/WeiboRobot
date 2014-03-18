package weibo.service;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:32
 * To change this template use File | Settings | File Templates.
 */
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import weibo.dao.*;

import java.util.Random;
public class ImWeibo {
    private static final Log logger = LogFactory.getLog(ImWeibo.class);
    private static int getUserNumber()throws Exception
    {
         return UserDAO.getMaxId();
    }

    public static void main(String[] args)
    {
        try{
            int usernumber=getUserNumber();
            WeiboUser[] weiboUsers=new WeiboUser[usernumber];
            Thread[] wUsers=new Thread[usernumber];
            for(int i=1;i<=usernumber;i++)
            {
              weiboUsers[i-1] = new WeiboUser(i);
                wUsers[i-1]=new Thread(weiboUsers[i-1]);
            }
            for(int i=1;i<=usernumber;i++)
            {
                wUsers[i-1].start();
            }
            UI ui=new UI(usernumber,weiboUsers);
            ui.setVisible(true);
            //WeiboUser weibouser=new WeiboUser(6);
            //weibouser.run();
            //测试爬网页
            /*int usernumber=getUserNumber();
            WeiboUser[] weiboUsers=new WeiboUser[usernumber];
            for(int i=1;i<=usernumber;i++)
            {
                weiboUsers[i-1] = new WeiboUser(i);
            }
            for(int i=1;i<=usernumber;i++)
            {
            weiboUsers[i-1].run();
            }*/

        }catch (Exception e){
            e.printStackTrace();
            logger.error(e);
        }
    }
}

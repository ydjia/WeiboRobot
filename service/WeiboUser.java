package weibo.service;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:27
 * To change this template use File | Settings | File Templates.
 */

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import weibo.dao.DataDAO;
import weibo.dao.WeiboDAO;
import weibo.model.*;
import weibo.dao.UserDAO;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;
import java.util.Properties;
import java.text.SimpleDateFormat;

public class WeiboUser implements Runnable{


    private static String SINA_PK = "EB2A38568661887FA180BDDB5CABD5F21C7BFD59C090CB2D24"
            + "5A87AC253062882729293E5506350508E7F9AA3BB77F4333231490F915F6D63C55FE2F08A49B353F444AD39"
            + "93CACC02DB784ABBB8E42A9B1BBFFFB38BE18D78E87A0E41B9B8F73A928EE0CCEE"
            + "1F6739884B9777E4FE9E88A1BBE495927AC4A799B3181D6442443";
    private  double phsend=0.3125;//高峰期发及转发数据库微博的概率，高峰期共4小时，平均发15条转10条，每5分钟左右执行一次
    private  double phforward=0.520833333;//高峰期转发主页微博的概率
    private  double plforward=0.020833;//低谷期转发主页微博的概率，低谷期共20小时，平均转3条，每5分钟左右执行一次
    private  double phfollow=0.601666667;//高峰期寻找一个关注
    private  double plfollow=0.53125;//关注粉丝
    private  double punfollow=0.604270833;//取消
    /*private  String username = "ebupt001@sina.cn";//默认账户
    private  String passwd = "2689621";
    private  String text = "默认微博";*/
    private  String Uniqueid=null;//新浪给每个用户的特定ID
    private  final Log logger = LogFactory.getLog(WeiboUser.class);
    private  User user;
    private  Weibo weibo;
    private  HttpClient client;
    private  UserDAO userdao;
    private  WeiboDAO weibodao;
    private  int weiboid=0;
    private  String max_mid="0";
    private  boolean stop;
//    private  String hightime="12-13,20-23";
    public WeiboUser(int userid)
    {
        try{
            user=new User();
            userdao=new UserDAO(user);
            userdao.getUserbyId(userid);
            this.user=userdao.getUser();
            weibo=new Weibo();
            weibodao=new WeiboDAO(weibo);
            int total=weibodao.getTotal(userid);
            client=getLoginStatus();
            //username=user.getName();
            //passwd=user.getPassword();
        }catch(Exception e){
            e.printStackTrace();
            logger.error(e);
        }
    }

    @Override
    public void run() {
        //模拟登陆、发微博、转发微博.
        while(true){
            try{

                if(issleeptime())
                    Thread.sleep(7200000);
                else{

                    Random random =new Random();
                    double ran=random.nextDouble();
                    logger.info("用户："+user.getId()+"执行,ran:"+ran+",高峰期："+ishightime());
                    if(ishightime()){
                        if(ran<phsend){
                            //client=getLoginStatus();
                            copy();//检查跟踪用户更新
                            sendWeibo();//高峰期平均转发10条微博
                            dataCollection();
                        }
                        else if(ran<phforward){
                            //client=getLoginStatus();
                            forwardweibo();//高峰期平均发布20条微博
                        }
                        else if(ran<plfollow){
                           // client=getLoginStatus();
                            followFans();//高峰期平均寻找1个关注
                        }
                        else if(ran<phfollow){
                            //client=getLoginStatus();
                            follow();//高峰期平均寻找1个关注
                        }
                        else if(ran<punfollow){
                            //client=getLoginStatus();
                            unfollow();//高峰期平均寻找1个关注
                        }
                    }
                    else{
                        if(ran<plforward){
                            //client=getLoginStatus();
                            forwardweibo();//低谷期平均转发5条微博
                        }
                    }
                    Thread.sleep(300000+new Double(80000*random.nextGaussian()).longValue());
                }
            }catch(Exception e){
                e.printStackTrace();
                logger.error(e);
            }
        }
        //sendWeibo();
        //client=getLoginStatus();
        //sendWeibo();
        //follow();
        //followFans();
        //forwardweibo();
        //comment();
        //copy();
        //unfollow();
        //dataCollection();
    }

    private HttpClient getLoginStatus() {

        final HttpClient client = HttpConnectionManager.getHttpClient();
        HttpPost post = new HttpPost(
                "http://login.sina.com.cn/sso/login.php?client=ssologin.js(v1.4.11)");
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        PreLoginInfo info = null;
        try {
            info = getPreLoginBean(client);
        } catch (HttpException e) {
            e.printStackTrace();
            logger.error(e);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("登陆失败，请确认已连接正确网络！" + e);
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error(e);
        }

        long servertime = info.servertime;
        String nonce = info.nonce;

        String pwdString = servertime + "\t" + nonce + "\n" + user.getPassword();
        String sp = new BigIntegerRSA().rsaCrypt(SINA_PK, "10001", pwdString);

        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("entry", "weibo"));
        nvps.add(new BasicNameValuePair("gateway", "1"));
        nvps.add(new BasicNameValuePair("from", ""));
        nvps.add(new BasicNameValuePair("savestate", "7"));
        nvps.add(new BasicNameValuePair("useticket", "1"));
        nvps.add(new BasicNameValuePair("ssosimplelogin", "1"));
        nvps.add(new BasicNameValuePair("vsnf", "1"));
        // new NameValuePair("vsnval", ""),
        nvps.add(new BasicNameValuePair("su", encodeUserName(user.getName())));
        nvps.add(new BasicNameValuePair("service", "miniblog"));
        nvps.add(new BasicNameValuePair("servertime", servertime + ""));
        nvps.add(new BasicNameValuePair("nonce", nonce));
        nvps.add(new BasicNameValuePair("pwencode", "rsa2"));
        nvps.add(new BasicNameValuePair("rsakv", info.rsakv));
        nvps.add(new BasicNameValuePair("sp", sp));
        nvps.add(new BasicNameValuePair("encoding", "UTF-8"));
        nvps.add(new BasicNameValuePair("prelt", "115"));
        nvps.add(new BasicNameValuePair("returntype", "META"));
        nvps.add(new BasicNameValuePair(
                "url",
                "http://weibo.com/ajaxlogin.php?framelogin=1&callback=parent.sinaSSOController.feedBackUrlCallBack"));
        try {
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response = client.execute(post);
            String entity = EntityUtils.toString(response.getEntity());
            if (entity.indexOf("code=0") == -1) {
                logger.error("用户名:"
                        + user.getName()+
                        "登陆失败:"
                        + URLDecoder.decode(entity.substring(
                        entity.indexOf("reason=") + 7,
                        entity.indexOf("&#39;\"/>")),"UTF-8"));
                System.out.println("登陆失败:"
                        + URLDecoder.decode(entity.substring(
                        entity.indexOf("reason=") + 7,
                        entity.indexOf("&#39;\"/>")),"UTF-8"));
                return null;
            }

            String url = entity.substring(
                    entity.indexOf("http://weibo.com/ajaxlogin.php?"),
                    entity.indexOf("code=0") + 6);

            HttpGet getMethod = new HttpGet(url);
            response = client.execute(getMethod);
            entity = EntityUtils.toString(response.getEntity());


            //userInfo.setUserid(entity.substring(entity.indexOf("\"userid\":"+9),entity.indexOf(",")));
            //userInfo.setUserid(entity.substring(entity.indexOf("\"displayname\":\""+15),entity.indexOf(",")));
            logger.info("用户："+user.getId()+",用户名:"
                    + user.getName()
                    + "登陆成功！\n"
                    );
            Uniqueid=entity.substring(entity.indexOf("uniqueid")+11,entity.indexOf("uniqueid")+21);

        } catch (ParseException e) {
            e.printStackTrace();
            logger.error(e);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return client;
    }

    private PreLoginInfo getPreLoginBean(HttpClient client)
            throws HttpException, IOException, JSONException {

        String serverTime = getPreLoginInfo(client);
        JSONObject jsonInfo = new JSONObject(serverTime);
        PreLoginInfo info = new PreLoginInfo();
        info.nonce = jsonInfo.getString("nonce");
        info.pcid = jsonInfo.getString("pcid");
        info.pubkey = jsonInfo.getString("pubkey");
        info.retcode = jsonInfo.getInt("retcode");
        info.rsakv = jsonInfo.getString("rsakv");
        info.servertime = jsonInfo.getLong("servertime");
        return info;
    }

    private String getPreLoginInfo(HttpClient client)
            throws ParseException, IOException {
        String preloginurl = "http://login.sina.com.cn/sso/prelogin.php?entry=sso&"
                + "callback=sinaSSOController.preloginCallBack&su="
                + "dW5kZWZpbmVk"
                + "&rsakt=mod&client=ssologin.js(v1.4.11)"
                + "&_=" + getCurrentTime();
        HttpGet get = new HttpGet(preloginurl);

        HttpResponse response = client.execute(get);

        String getResp = EntityUtils.toString(response.getEntity());

        int firstLeftBracket = getResp.indexOf("(");
        int lastRightBracket = getResp.lastIndexOf(")");

        String jsonBody = getResp.substring(firstLeftBracket + 1,
                lastRightBracket);
        // System.out.println(jsonBody);
        return jsonBody;

    }

    private String getCurrentTime() {
        long servertime =System.currentTimeMillis() / 1000;
        return String.valueOf(servertime);
    }

    private String encodeUserName(String email) {
        email = email.replaceFirst("@", "%40");// MzM3MjQwNTUyJTQwcXEuY29t
        email = Base64.encodeBase64String(email.getBytes());
        return email;
    }
    private boolean issleeptime(){  //判断是否为睡觉时间
            Date d = new Date();               //获取时间，now为现在时间，b为起始时间，e为结束时间
            Calendar now = Calendar.getInstance();
            Calendar b = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            now.setTime(d);
            b.setTime(d);
            e.setTime(d);
            b.set(Calendar.HOUR_OF_DAY, 2);
            e.set(Calendar.HOUR_OF_DAY, 7);
            b.set(Calendar.MINUTE, 0);
            e.set(Calendar.MINUTE, 0);
            b.set(Calendar.SECOND, 0);
            e.set(Calendar.SECOND, 0);
            if(b.before(now)&&e.after(now))
                return true;
            else
                return false;
    }
    private boolean ishightime(){  //判断是否为高峰期
        String[] htime=user.getHightime().split(",");   //高峰期格式为“xx-xx,xx-xx,xx-xx...”
        for(int i=0;i<htime.length;i++){
            String[] hhtime=htime[i].split("-");
            int begin=(Integer.parseInt(hhtime[0]));
            int end=(Integer.parseInt(hhtime[1]));
            Date d = new Date();               //获取时间，now为现在时间，b为高峰期起始时间，e为高峰期结束时间
            Calendar now = Calendar.getInstance();
            Calendar b = Calendar.getInstance();
            Calendar e = Calendar.getInstance();
            now.setTime(d);
            b.setTime(d);
            e.setTime(d);
            b.set(Calendar.HOUR_OF_DAY, begin);
            e.set(Calendar.HOUR_OF_DAY, end);
            b.set(Calendar.MINUTE, 0);
            e.set(Calendar.MINUTE, 0);
            b.set(Calendar.SECOND, 0);
            e.set(Calendar.SECOND, 0);
            if(b.before(now)&&e.after(now))
                return true;
            else
                continue;
        }
        return false;
    }
    private void sendWeibo()
    {
        String sendweibourl = "http://weibo.com/aj/mblog/add?_wv=5&__rnd=";
        String forwardurl="http://weibo.com/aj/mblog/forward?_wv=5&__rnd=";
        sendweibourl +=System.currentTimeMillis();
        forwardurl+=System.currentTimeMillis();
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        try{
            Properties property = new Properties();
            property.load(new FileInputStream("src/user.properties"));
            weiboid=Integer.parseInt(property.getProperty("weiboid"+user.getId()))+1;
            //weiboid=weiboid+1;
            weibodao.getWeibobyId(weiboid,user.getId());
            weibo=weibodao.getWeibo();
            //text=this.weibo.getText();
            //thumbnail/979bffddgw1e5aywscoj8j20c81qd4d4.jpg
            String pic_id="";
            if(weibo.getPid().equals("")){
                if(!weibo.getImg_link().equals("")){
                   Matcher m = Pattern.compile("(?<=thumbnail/).*(?=\\.)|(?<=square/).*(?=\\.)")
                            .matcher(weibo.getImg_link());
                   while(m.find()){
                     pic_id=m.group();
                   }
                }
                HttpPost post=new HttpPost(sendweibourl);
                post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                post.addHeader("Accept-Encoding", "gzip, deflate");
                post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
                post.addHeader("Cache-Control", "no-cache");
                post.addHeader("Connection", "keep-alive");
                post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                post.addHeader("Host", "weibo.com");
                post.addHeader("Pragma", "no-cache");
                post.addHeader("Referer", "http://weibo.com/u/"+Uniqueid);
                post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
                post.addHeader("X-Requested-With", "XMLHttpRequest");
                nvps.add(new BasicNameValuePair("pic_id", pic_id));
                nvps.add(new BasicNameValuePair("rank", "0"));
                nvps.add(new BasicNameValuePair("rankid", ""));
                nvps.add(new BasicNameValuePair("_surl", ""));
                nvps.add(new BasicNameValuePair("hottopicid", ""));
                nvps.add(new BasicNameValuePair("location", "home"));
                nvps.add(new BasicNameValuePair("module", "stissue"));
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("text", weibo.getText()));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response = client.execute(post);
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "发布"+weibo.getText()+"成功\n"
                    );
                    property.setProperty("weiboid",String.valueOf(weiboid));
                    property.store(new FileOutputStream("src/user"+user.getId()+".properties"),"src/user"+user.getId()+".properties");
                }
                else{
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "发布失败\n"
                    );
                }
            }
            else {
                HttpPost post=new HttpPost(forwardurl);
                post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                post.addHeader("Accept-Encoding", "gzip, deflate");
                post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
                post.addHeader("Cache-Control", "no-cache");
                post.addHeader("Connection", "keep-alive");
                post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                post.addHeader("Host", "weibo.com");
                post.addHeader("Pragma", "no-cache");
                post.addHeader("Referer", "http://weibo.com/u/"+user.getCopywho()+"?from=profile&wvr=5&loc=myfollow");
                post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
                post.addHeader("X-Requested-With", "XMLHttpRequest");
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("appkey", ""));
                nvps.add(new BasicNameValuePair("location", "profile"));
                nvps.add(new BasicNameValuePair("mark", ""));
                nvps.add(new BasicNameValuePair("mid", weibo.getPid()));
                nvps.add(new BasicNameValuePair("module", "tranlayout"));
                nvps.add(new BasicNameValuePair("rank", "0"));
                nvps.add(new BasicNameValuePair("rankid", ""));
                nvps.add(new BasicNameValuePair("reason", weibo.getText()));
                nvps.add(new BasicNameValuePair("style_type", "1"));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response = client.execute(post);
                if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "从数据库转发"+weibo.getText()+"成功\n"
                    );
                    property.setProperty("weiboid"+user.getId(),String.valueOf(weiboid));
                    property.store(new FileOutputStream("src/user.properties"),"src/user.properties");
                }
                else{
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "从数据库转发失败\n"
                    );
                }
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
    private void forwardweibo()
    {
        String homeurl = "http://weibo.com/u/";
        homeurl +=Uniqueid;
        HttpGet get=new HttpGet(homeurl);
        String mids="";
        Random random =new Random();
        String forwardurl="http://weibo.com/aj/mblog/forward?_wv=5&__rnd=";
        forwardurl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(forwardurl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", "http://weibo.com/u/"+Uniqueid+"?wvr=5&wvr=5&lf=reg");
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");


        try{
        HttpResponse response = client.execute(get);
        String entity = EntityUtils.toString(response.getEntity());
        if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
        logger.info("用户:"+user.getId()+",uid="
                + Uniqueid
                + "home！\n"
                );
        }
        else{
        logger.info("用户:"+user.getId()+",uid="
                + Uniqueid
                + "home失败！\n"
        );
        }
        Matcher m = Pattern.compile("(?<=mid=)\\d{16}(?=\\\\\")")
                    .matcher(entity);

        while(m.find())
        {
          mids+=m.group()+",";
        }
        String b[]=mids.split(",");
        int index=random.nextInt(b.length);
        String mid=b[index];
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("_t", "0"));
        nvps.add(new BasicNameValuePair("appkey", ""));
        nvps.add(new BasicNameValuePair("location", "home"));
        nvps.add(new BasicNameValuePair("mark", ""));
        nvps.add(new BasicNameValuePair("mid",mid));
        nvps.add(new BasicNameValuePair("module", "tranlayout" ));
        nvps.add(new BasicNameValuePair("rank", "0"));
        nvps.add(new BasicNameValuePair("rankid", ""));
        nvps.add(new BasicNameValuePair("reason", "转发微博"));
        nvps.add(new BasicNameValuePair("style_type", "1"));
        post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
        HttpResponse response1 = client.execute(post);
        if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
            logger.info("用户:"+user.getId()+",uid="
                    + Uniqueid
                    + "转发"+mid+"成功！\n"
            );
        }
        else{
            logger.info("用户:"+user.getId()+",uid="
                    + Uniqueid
                    + "转发"+mid+"失败！\n"
            );
        }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
    private void comment()
    {
        String homeurl = "http://weibo.com/u/";
        homeurl +=Uniqueid;
        HttpGet get=new HttpGet(homeurl);
        String mids="";
        Random random =new Random();
        String commenturl="http://weibo.com/aj/comment/add?_wv=5&__rnd=";
        commenturl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(commenturl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", "http://weibo.com/u/"+Uniqueid+"?leftnav=1&wvr=5");
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");


        try{
            HttpResponse response = client.execute(get);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "home！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "home失败！\n"
                );
            }
            Matcher m = Pattern.compile("(?<=mid=)\\d{16}(?=\\\\\")")
                    .matcher(entity);

            while(m.find())
            {
                mids+=m.group()+",";
            }
            String b[]=mids.split(",");
            int index=random.nextInt(b.length);
            String mid=b[index];
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            weiboid=weiboid+1;
            weibo=new Weibo();
            weibodao=new WeiboDAO(weibo);
            weibodao.getWeibobyId(weiboid,user.getId());
            this.weibo=weibodao.getWeibo();
            //text=this.weibo.getText();
            nvps.add(new BasicNameValuePair("_t", "0"));
            nvps.add(new BasicNameValuePair("act", "post"));
            nvps.add(new BasicNameValuePair("content", weibo.getText()));
            nvps.add(new BasicNameValuePair("forward", "0"));
            nvps.add(new BasicNameValuePair("groupsource", "group_all"));
            nvps.add(new BasicNameValuePair("isroot", "0"));
            nvps.add(new BasicNameValuePair("location", "home" ));
            nvps.add(new BasicNameValuePair("mid", mid));
            nvps.add(new BasicNameValuePair("module", "scommlist"));
            nvps.add(new BasicNameValuePair("repeatNode", "[object HTMLDivElement]"));
            nvps.add(new BasicNameValuePair("type", "big"));
            nvps.add(new BasicNameValuePair("uid", Uniqueid));
            post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            HttpResponse response1 = client.execute(post);
            if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "评论"+mid+"成功！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "评论"+mid+"失败！\n"
                );
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
    private void follow(){
        //(?<=uid=\\")\d{10}.*?action-type=\\"follow\\".*?W_addbtn
        //(?<=uid=\\")\d{10}.*?action-type=\\"follow\\".*?W_addbtn(?!_es)
        int page=1;
        String uids="";
        String furl="http://s.weibo.com/user/";
        try{
        furl+= URLEncoder.encode(user.getKeyword(),"UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            logger.error(e);
        }
        furl=furl+"&Refer=SUer_box";
        HttpGet get1=new HttpGet(furl);
        Random random =new Random();
        String followurl="http://s.weibo.com/ajax/user/follow?__rnd=";
        followurl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(followurl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "s.weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", furl);
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");


        try{
            HttpResponse response = client.execute(get1);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取兴趣用户列表成功！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取兴趣用户列表失败！\n"
                );
            }
            Matcher m = Pattern.compile("(?<=uid=\\\\\")\\d{10}.*?action-type=\\\\\"follow\\\\\".*?W_addbtn(?!_es)")
                    .matcher(entity);

            while(m.find())
            {
                uids+=m.group().substring(0,10)+",";
            }
            String b[]=uids.split(",");
            int index=random.nextInt(b.length);
            if(b.length != 0){
                String uid=b[index];
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("type", "followed"));
                nvps.add(new BasicNameValuePair("uid", uid));
                nvps.add(new BasicNameValuePair("wforce", "0"));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response1 = client.execute(post);
                if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"成功！\n"
                    );
                }
                else{
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"失败！\n"
                    );
                }
            }
            else{
                follow(page);
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }

    }
    private void follow(int page){
        String uids="";
        page++;
        String furl="http://s.weibo.com/user/";
        try{
            furl+= URLEncoder.encode(user.getKeyword(),"UTF-8");
        }catch (UnsupportedEncodingException e){
            e.printStackTrace();
            logger.error(e);
        }
        furl=furl+"&page="+page;
        HttpGet get1=new HttpGet(furl);
        Random random =new Random();
        String followurl="http://s.weibo.com/ajax/user/follow?__rnd=";
        followurl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(followurl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "s.weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", furl);
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");


        try{
            HttpResponse response = client.execute(get1);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取兴趣用户列表成功！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取兴趣用户列表失败！\n"
                );
            }
            Matcher m = Pattern.compile("(?<=uid=\\\\\")\\d{10}.*?action-type=\\\\\"follow\\\\\".*?W_addbtn(?!_es)")
                    .matcher(entity);

            while(m.find())
            {
                uids+=m.group().substring(0,10)+",";
            }
            String b[]=uids.split(",");
            int index=random.nextInt(b.length);
            if(!b.equals("")){
                String uid=b[index];
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("type", "followed"));
                nvps.add(new BasicNameValuePair("uid", uid));
                nvps.add(new BasicNameValuePair("wforce", "0"));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response1 = client.execute(post);
                if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"成功！\n"
                    );
                }
                else{
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"失败！\n"
                    );
                }
            }
            else{
                follow(page);
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }

    }
    private void followFans(){
        String uids="";
        String furl="http://weibo.com/"+Uniqueid+"/myfans?f=3&wvr=5";
        HttpGet get1=new HttpGet(furl);
        Random random =new Random();
        String followurl="http://s.weibo.com/ajax/user/follow?__rnd=";
        followurl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(followurl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "s.weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", furl);
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");


        try{
            HttpResponse response = client.execute(get1);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取未关注粉丝列表成功！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取未关注粉丝列表失败！\n"
                );
            }
            //(?<=img\susercard=\\"id=)\d{10}
            Matcher m = Pattern.compile("(?<=img\\susercard=\\\\\"id=)\\d{10}")
                    .matcher(entity);

            while(m.find())
            {
                uids+=m.group().substring(0,10)+",";
            }
            String b[]=uids.split(",");
            if(!b[0].equals("")){
                int index=random.nextInt(b.length);
                String uid=b[index];
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("type", "followed"));
                nvps.add(new BasicNameValuePair("uid", uid));
                nvps.add(new BasicNameValuePair("wforce", "0"));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response1 = client.execute(post);
                if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"成功！\n"
                    );
                }
                else{
                    logger.info("用户:"+user.getId()+",uid="
                            + Uniqueid
                            + "关注"+uid+"失败！\n"
                    );
                }
            }
            else
            logger.info("无未关注的粉丝\n");
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }

    }
    private void unfollow(){
        String pg_count;
        String furl="http://weibo.com/"+Uniqueid+"/myfollow?page=1&t=3&ftype=0";
        HttpGet get1=new HttpGet(furl);
        get1.addHeader("Accept", "text/html/javascript,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        get1.addHeader("Connection", "keep-alive");
        try {
            HttpResponse response = client.execute(get1);
            String text = response.toString();
            if(HttpStatus.SC_OK==response.getStatusLine().getStatusCode()){
                //请求成功
                //取得请求内容
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // 显示结果
                    String html = EntityUtils.toString(entity,"GBK");
                    Document doc = Jsoup.parse(html,furl);
                    Elements focuss = doc.select("script");

                    int count = 0;
                    for (Element focus:focuss)
                    {
                        ++count;
                        if(count == 7){
                            String html1 = focus.toString();
                            String json = html1.substring(html1.indexOf('{'),html1.lastIndexOf(')'));
                            JSONObject jsonObject = new JSONObject(json);
                            String json_html = jsonObject.getString("html");
                            Document doc1 = Jsoup.parse(json_html);
                            Element ele1 = doc1.select("div.W_pages_minibtn>a.page").last();
                            if(ele1!=null)
                                pg_count=ele1.text();

                            else
                                pg_count="1";
                            System.out.print("页数");
                            System.out.println(pg_count);
                            unfollow(Integer.parseInt(pg_count));
                        }
                    }
                }
            }
        }catch(IOException e){
            e.printStackTrace();
            logger.error(e);
        }catch (JSONException e){
            e.printStackTrace();
            logger.error(e);
        }
    }
    private void unfollow(int page){
        String uids="";
        String furl="http://weibo.com/"+Uniqueid+"/myfollow?page="+page+"&t=3&ftype=0";
        HttpGet get1=new HttpGet(furl);
        String unfollowurl="http://weibo.com/aj/f/unfollow?_wv=5&__rnd=";
        unfollowurl+=System.currentTimeMillis();
        HttpPost post=new HttpPost(unfollowurl);
        post.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        post.addHeader("Accept-Encoding", "gzip, deflate");
        post.addHeader("Accept-Language", "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3");
        post.addHeader("Cache-Control", "no-cache");
        post.addHeader("Connection", "keep-alive");
        post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        post.addHeader("Host", "s.weibo.com");
        post.addHeader("Pragma", "no-cache");
        post.addHeader("Referer", furl);
        post.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:22.0) Gecko/20100101 Firefox/22.0");
        post.addHeader("X-Requested-With", "XMLHttpRequest");
        try{
            HttpResponse response = client.execute(get1);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取关注列表"+page+"成功！\n"
                );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取关注列表"+page+"失败！\n"
                );
            }
            //(?<=usercard=\\"id=)\d{10}(?=\\"\salt)
            Matcher m = Pattern.compile("(?<=usercard=\\\\\"id=)\\d{10}(?=\\\\\"\\salt)")
                    .matcher(entity);

            while(m.find())
            {
                uids+=m.group().substring(0,10)+",";
            }
            String b[]=uids.split(",");
            if(!b[0].equals("")){
                String uid=b[b.length-1];
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();
                nvps.add(new BasicNameValuePair("_t", "0"));
                nvps.add(new BasicNameValuePair("location", "myfollow"));
                nvps.add(new BasicNameValuePair("refer_flag","unfollow"));
                nvps.add(new BasicNameValuePair("refer_sort", "relationManage"));
                nvps.add(new BasicNameValuePair("uid", uid));
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
                HttpResponse response1 = client.execute(post);
                    if(response1.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                        logger.info("用户:"+user.getId()+",uid="
                                + Uniqueid
                                + "取消关注"+uid+"成功！\n"
                        );
                    }
                    else{
                        logger.info("用户:"+user.getId()+",uid="
                                + Uniqueid
                                + "取消关注"+uid+"失败！\n"
                        );
                    }
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "无可取消关注！\n"
                );
            }
        }catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (ClientProtocolException e) {
            e.printStackTrace();
            logger.error(e);
        }catch (IOException e) {
            e.printStackTrace();
            logger.error(e);
        }
        catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
    }
    private void copy(){
        //String url_str = "http://weibo.com/u/"+user.getCopywho();
        String url_str = "http://weibo.com/p/100505"+user.getCopywho()+"/weibo?from=page_100505_home&wvr=5.1&mod=weibomore&ajaxpagelet=1&__ref=/u/"+user.getCopywho()+"&_t=FM_137569252749618";
        HttpGet getMethod = new HttpGet(url_str);
        getMethod.addHeader("Accept", "text/html/javascript,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        getMethod.addHeader("Connection", "keep-alive");
        try {
        HttpResponse response = client.execute(getMethod);
        String text = response.toString();
            if(HttpStatus.SC_OK==response.getStatusLine().getStatusCode()){
                //请求成功
                //取得请求内容
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    // 显示结果
                    String html = EntityUtils.toString(entity,"GBK");
                    /*File file = new File("test"+user.getId()+".txt");
                    BufferedWriter out = new BufferedWriter(new FileWriter(file, false));
                    out.write(html);*/
                    Document doc = Jsoup.parse(html,url_str);
                    Elements focuss = doc.select("script");

                    int count = 0;
                    for (Element focus:focuss)
                    {
                        ++count;
                        if(count == 6)
                        {

                            String html1 = focus.toString();
                            String json = html1.substring(html1.indexOf('{'),html1.lastIndexOf(')'));
                            JSONObject jsonObject = new JSONObject(json);
                            String json_html = jsonObject.getString("html");
                            max_mid=weibodao.getMaxMid(user.getId());
                            analyze(json_html);
                        }
                    }
                }

            }

        }catch(IOException e){
            e.printStackTrace();
            logger.error(e);
        }catch (JSONException e){
            e.printStackTrace();
            logger.error(e);
        }catch (Exception e){
            e.printStackTrace();
            logger.error(e);
        }
        load();
    }
    private void load(){
        if(stop)
            return;
        String url_str = "";
        int n;     //加载次数
        int page = 1; //当前页
        int pre_page;
        int pg_count = 1;//当前微博总页数
        for(n = 2;n <= 3;n++)
        {
            if(n == 2)
                url_str = "http://weibo.com/aj/mblog/mbloglist?_wv=5&page=1&count=15&max_id=3596088839751484&pre_page=1&end_id=3601842677751273&pagebar=0&_k=137424093123926&uid="+user.getCopywho()+"&_t=0&__rnd=1374240979876";
            else if(!stop)
                url_str = "http://weibo.com/aj/mblog/mbloglist?_wv=5&page=1&count=15&max_id=3596088839751484&pre_page=1&end_id=3601842677751273&pagebar=1&_k=137424093123926&uid="+user.getCopywho()+"&_t=0&__rnd=1374240979876";

            HttpGet getMethod = new HttpGet(url_str);
            getMethod.addHeader("Accept", "text/html/javascript,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            getMethod.addHeader("Connection", "keep-alive");

            try {
                HttpResponse response = client.execute(getMethod);
                String text = response.toString();
                if(HttpStatus.SC_OK==response.getStatusLine().getStatusCode()){
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String html = EntityUtils.toString(entity,"GBK");
                        String json = html.substring(html.indexOf('{'),html.lastIndexOf('}')+1);
                        JSONObject jsonObject = new JSONObject(json);
                        String json_html = jsonObject.getString("data");
                        if(n == 3){  //取得该微博总页数
                            Document doc2 = Jsoup.parse(json_html);
                            Element ele8 = doc2.select("div.W_pages>span.list").first();
                            if(ele8 != null){
                                String l = ele8.text();
                                String l_s = l.substring(2,(l.indexOf('\u9875'))-1);
                                pg_count = Integer.parseInt(l_s);
                            }
                            else
                                pg_count = 1;
                                System.out.print("页数");
                                System.out.println(pg_count);
                        }
                        analyze(json_html);
                    }
                }
            } catch(MalformedURLException e) {
            } catch(IOException e) {
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if(pg_count != 1){
            for(n = 4;n <= 3*pg_count&&!stop;n++)
            {
                if(n%3 == 1){
                    ++page;
                    pre_page = page - 1;
                    url_str = "http://weibo.com/aj/mblog/mbloglist?_wv=5&page="+page+"&count=50&pre_page="+pre_page+"&end_id=3601842677751273&_k=137424624162629&_t=0&end_msign=-1&uid="+user.getCopywho()+"&__rnd=1374246339681";
                }
                else if(n%3 == 2)
                    url_str = "http://weibo.com/aj/mblog/mbloglist?_wv=5&page="+page+"&count=15&pre_page="+page+"&end_id=3601842677751273&_k=137424624162649&_t=0&max_id=3578244554724754&pagebar=0&uid="+user.getCopywho()+"&__rnd=1374246536903";
                else
                    url_str = "http://weibo.com/aj/mblog/mbloglist?_wv=5&page="+page+"&count=15&pre_page="+page+"&end_id=3601842677751273&_k=137424624162654&_t=0&max_id=3571302181318729&pagebar=1&uid="+user.getCopywho()+"&__rnd=1374246599076";
                HttpGet getMethod = new HttpGet(url_str);
                getMethod.addHeader("Accept", "text/html/javascript,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                getMethod.addHeader("Connection", "keep-alive");

                try {
                    HttpResponse response = client.execute(getMethod);
                    String text = response.toString();
                    if(HttpStatus.SC_OK==response.getStatusLine().getStatusCode()){
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            String html = EntityUtils.toString(entity,"GBK");
                            String json = html.substring(html.indexOf('{'),html.lastIndexOf('}')+1);
                            JSONObject jsonObject = new JSONObject(json);
                            String json_html = jsonObject.getString("data");
                            analyze(json_html);
                        }
                    }
                } catch(MalformedURLException e) {
                } catch(IOException e) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
    private void analyze(String html){

        Document doc1 = Jsoup.parse(html);
        Elements eles = doc1.select("div.WB_detail");

        for (Element ele:eles)
        {
            //Elements eles5 = ele.select("div.WB_handle");
            try{
                Thread.sleep(200);
                Elements eles6 = ele.getElementsByTag("a");
                int found_pid = 0;
                for(Element ele6:eles6)
                {
                    String id = ele6.attr("action-data");
                    String regMid = "&mid=(\\d+)&name=";
                    String regPid = "&rootmid=(\\d+)&rootname";
                    Pattern patternMid = Pattern.compile(regMid);
                    Pattern patternPid = Pattern.compile(regPid);
                    Matcher matcherMid = patternMid.matcher(id);
                    Matcher matcherPid = patternPid.matcher(id);
                    String pid = "";
                    String mid = "";
                    if (matcherPid.find()) {
                        found_pid = 1;
                        pid = matcherPid.group(1);
                        weibo.setPid(pid);
                    }
                    if (matcherMid.find()) {
                        mid = matcherMid.group(1);
                        weibo.setMid(mid);
                        if(weibo.getMid().compareTo(max_mid)<=0){
                            stop = true;
                            weibo.setText("");
                            weibo.setImg_link("");
                            weibo.setTime("");
                            weibo.setPid("");
                            weibo.setMid("0");
                            return;
                        }
                    }
                }
                if(found_pid == 0)
                    weibo.setPid("");
                else
                    found_pid = 0;
                Elements eles7 = ele.select("div.WB_info");
                for(Element ele7:eles7)
                {
                    String linkText = ele7.text();
                    weibo.setText(linkText);
                }
                Elements eles1 = ele.select("div.WB_text");
                for(Element ele1:eles1)
                {
                    String linkText = ele1.text();
                    weibo.setText(weibo.getText()+linkText);
                }
                Elements eles2 = ele.select("div.WB_from>a.S_link2");
                int t_count = 0;
                for(Element ele2:eles2)
                {
                    ++t_count;
                    String linkText = ele2.text();
                    if(t_count == 1){
                        String regT1 = "今天\\s(\\w+:\\w+)";
                        String regT2 = "(\\w+)月(\\w+)日\\s(\\w+:\\w+)";
                        String regT3 = "(\\d+)分钟前";
                        Pattern patternT1 = Pattern.compile(regT1);
                        Pattern patternT2 = Pattern.compile(regT2);
                        Pattern patternT3 = Pattern.compile(regT3);
                        Matcher matcherT1 = patternT1.matcher(linkText);
                        Matcher matcherT2 = patternT2.matcher(linkText);
                        Matcher matcherT3 = patternT3.matcher(linkText);
                        if (matcherT1.find()) {
                            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");//设置日期格式
                            //System.out.println("今天");
                            //System.out.println(df.format(new Date())+" "+matcherT1.group(1));
                            weibo.setTime(df.format(new Date())+" "+matcherT1.group(1));
                        }
                        else if (matcherT2.find()) {
                            SimpleDateFormat df = new SimpleDateFormat("yyyy");//设置日期格式
                            //System.out.println("年月");
                            //System.out.println(df.format(new Date())+"-"+matcherT2.group(1)+"-"+matcherT2.group(2)+" "+matcherT2.group(3));
                            weibo.setTime(df.format(new Date())+"-"+matcherT2.group(1)+"-"+matcherT2.group(2)+" "+matcherT2.group(3));
                        }
                        else if (matcherT3.find()) {

                            Date date=new Date();
                            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            java.util.Calendar Cal=java.util.Calendar.getInstance();
                            Cal.setTime(date);
                            int t = Integer.parseInt(matcherT3.group(1));
                            Cal.add(java.util.Calendar.MINUTE,-t);   //这里可以当前时间 年 月 日 时 分 加 减
                            //System.out.println("分钟前");
                            //System.out.println(format.format(Cal.getTime()));
                            weibo.setTime(format.format(Cal.getTime()));
                        }
                        else{
                            //System.out.println("正常");
                            weibo.setTime(linkText);
                        }
                    }
                }
                t_count = 0;
                Elements eles3 = ele.select("img.bigcursor");
                for(Element ele3:eles3)
                {
                    String linkHref = ele3.attr("src");
                    weibo.setImg_link(linkHref);
                }
                if (weibo.getTime()!="")
                    weibodao.saveWeibo(user.getId());
                    weibo.setText("");
                    weibo.setImg_link("");
                    weibo.setTime("");
                    weibo.setPid("");
                    weibo.setMid("0");
            }
            catch (Exception e){
                e.printStackTrace();
                logger.error(e);
            }

        }
    }
    public void  dataCollection(){
        String dataurl = "http://data.weibo.com/mydata";
        HttpGet get=new HttpGet(dataurl);
        try{
            Data data=new Data();
            DataDAO dataDAO=new DataDAO(data);
            HttpResponse response = client.execute(get);
            String entity = EntityUtils.toString(response.getEntity());
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取数据中...\n"
                );
                Document doc1 = Jsoup.parse(entity);
                Element e1=doc1.select("ul.user_atten").first();
                Elements e2=e1.select("strong");
                int i=0;
                for (Element e3:e2){
                    if(i==0)
                        data.setFollows(Integer.parseInt(e3.text()));
                    if(i==1)
                        data.setFans(Integer.parseInt(e3.text()));
                    if(i==2)
                        data.setWeibos(Integer.parseInt(e3.text()));
                    i++;
                }
                Element e4=doc1.select("div.vagility").first();
                Elements e5=e4.select("span.num");
                int j=0;
                for (Element e6:e5){
                    if(j==0)
                        data.setForwards(Integer.parseInt(e6.text()));
                    if(j==1)
                        data.setComments(Integer.parseInt(e6.text()));
                    j++;
                }
                dataDAO.setData(user.getId());
                logger.info("用户:"+user.getId()+",uid="+ Uniqueid + "数据：\n"
                +"[关注量："+data.getFollows()+"],[粉丝量："+data.getFans()+"],[微博量："+data.getWeibos()+"]\n"
                +"[原创被转发："+data.getForwards()+"],[原创被评论："+data.getComments()+"]\n"  );
            }
            else{
                logger.info("用户:"+user.getId()+",uid="
                        + Uniqueid
                        + "获取数据失败！\n"
                );
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
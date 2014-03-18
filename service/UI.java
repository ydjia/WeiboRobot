package weibo.service;

import weibo.dao.DataDAO;
import weibo.model.Data;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-8-21
 * Time: 下午5:00
 * To change this template use File | Settings | File Templates.
 */
public class UI extends JFrame{
    private JTextArea[] infos;
    private JButton refresh;
    private DataDAO dataDAO;
    private Data data;
    private Container c;
    private int num;
    private WeiboUser[] weiboUsers;
    public UI(int num,WeiboUser[] weiboUsers){
        super();
        this.num=num;
        this.weiboUsers=weiboUsers;
        c = this.getContentPane();
        c.setLayout(new FlowLayout(3));
        getAll(this.num,this.weiboUsers);
    }
    public void getAll(int num,WeiboUser[] weiboUsers){
        infos=new JTextArea[num];
        try{
            data=new Data();
            dataDAO=new DataDAO(data);
            for (int i=1;i<=num;i++){
                infos[i-1]=new JTextArea();
                weiboUsers[i-1].dataCollection();
                dataDAO.getData(i);
                infos[i-1].setText("用户"+i+"\n[关注量："+data.getFollows()+"],[粉丝量："+data.getFans()+"],[微博量："+data.getWeibos()+"]\n"
                        +"[原创被转发："+data.getForwards()+"],[原创被评论："+data.getComments()+"]\n");
                infos[i-1].setPreferredSize(new Dimension(230,65));
                c.add(infos[i - 1]);
            }
            refresh=new JButton("刷新");
            refresh.addActionListener(new myActionListen());
            c.add(refresh);
            this.setTitle("机器人状态");
            this.setBounds(200,0,1000,700);
            this.setVisible(true);
            this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        }catch (Exception e){
            e.printStackTrace();
        }

    }
    public class myActionListen implements java.awt.event.ActionListener {
        public void actionPerformed(ActionEvent e) {
             c.removeAll();
             getAll(num,weiboUsers);
        }
    }
}

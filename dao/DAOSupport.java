package weibo.dao;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午12:55
 * To change this template use File | Settings | File Templates.
 */
import java.sql.Connection;
import java.sql.DriverManager;

public class DAOSupport{
    protected static java.sql.Connection connection;
    protected static Connection getConnection() throws Exception
    {
        Class.forName("com.mysql.jdbc.Driver");
        // 获得Connection对象
        Connection conn = DriverManager.getConnection(
                "jdbc:mysql://localhost/imweibo?characterEncoding=UTF8", "root",
                "1993");
        return conn;
    }
    public DAOSupport()throws Exception
    {
        this.connection = getConnection();
    }
}

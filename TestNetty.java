import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;

public class TestNetty {
    public static void main(String[] args) {
        try {
            System.out.println("Trying to load Netty classes...");
            EventLoopGroup group = new NioEventLoopGroup();
            System.out.println("Netty classes loaded successfully!");
            group.shutdownGracefully();
        } catch (ClassNotFoundException e) {
            System.out.println("Cannot find Netty classes: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Error loading Netty classes: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
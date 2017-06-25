package cn.worldwalker.game.jinhua.common.constant;

public class Constant {
	
	/**roomId与roomInfo的映射*/
	public final static String jinhuaRoomIdRoomInfoMap = "jinhua_room_id_room_info_map";
	/**roomId与msgId的映射关系*/
	public final static String jinhuaRoomIdMsgIdMap = "jinhua_room_id_msg_id_map";
	/**playerId与roomId的映射*/
	public final static String jinhuaPlayerIdRoomIdMap = "jinhua_player_id_room_id_map";
	/**offline playerId与roomId的映射关系*/
	public final static String jinhuaOfflinePlayerIdTimeMap = "jinhua_offline_player_id_time_map";
	/**ip与此ip上连接数的映射关系*/
	public final static String jinhuaIpConnectCountMap = "jinhua_ip_connect_count_map";
	/**房卡操作失败数据list*/
	public final static String jinhuaRoomCardOperationFailList = "jinhua_room_card_operation_fail_list";
	
	/**请求和返回信息日志打印开关*/
	public final static String jinhuaLogInfoFuse = "jinhua_log_info_fuse";
	/**登录切换开关*/
	public final static String jinhuaLoginFuse = "jinhua_login_fuse";
	/**roomLockPrefix*/
	public final static String jinhuaRoomLockPrefix = "jinhua_room_lock_prefix_";
	
	
	/**底注*/
	public final static Integer stakeButtom = 1;
	/**押注的上限*/
	public final static Integer stakeLimit = 10;
	/**跟注次数上限*/
	public final static Integer stakeTimesLimit = 6;
	
	 // // 第三方用户唯一凭证
    public static String appId = "wx7681f478b552345a";
    // // 第三方用户唯一凭证密钥
    public static String appSecret = "aa78590d5de26457296c3dd4fc8c1a14";
	
	public static String getWXUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo?access_token=" + "ACCESS_TOKEN&openid=OPENID";
	
	public static String getOpenidAndAccessCode = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId + "&secret=" + appSecret + "&grant_type=authorization_code&code=CODE";

	
}

package cn.worldwalker.game.jinhua.service.game;

import io.netty.channel.ChannelHandlerContext;
import cn.worldwalker.game.jinhua.domain.game.GameRequest;
import cn.worldwalker.game.jinhua.domain.result.Result;

public interface GameService {
	
	public Result login(String token, String deviceType);
	
	public Result getIpByRoomId(String token, Long roomId);
	
	public Result entryHall(ChannelHandlerContext ctx, GameRequest request);
	
	public Result createRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result entryRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result ready(ChannelHandlerContext ctx, GameRequest request);
	
	public Result dealCards(ChannelHandlerContext ctx, GameRequest request);
	
	public Result stake(ChannelHandlerContext ctx, GameRequest request);
	
	public Result watchCards(ChannelHandlerContext ctx, GameRequest request);
	
	public Result manualCardsCompare(ChannelHandlerContext ctx, GameRequest request);
	
	public Result discardCards(ChannelHandlerContext ctx, GameRequest request);
	
	public Result curSettlement(ChannelHandlerContext ctx, GameRequest request);
	
	public Result totalSettlement(ChannelHandlerContext ctx, GameRequest request);
	
	public Result dissolveRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result agreeDissolveRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result disagreeDissolveRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result refreshRoom(ChannelHandlerContext ctx, GameRequest request);
	
	public Result delRoomConfirmBeforeReturnHall(ChannelHandlerContext ctx, GameRequest request);
	
	
	
}

package cn.worldwalker.game.jinhua.service.game.job;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cn.worldwalker.game.jinhua.common.constant.Constant;
import cn.worldwalker.game.jinhua.common.utils.JsonUtil;
import cn.worldwalker.game.jinhua.domain.enums.MsgTypeEnum;
import cn.worldwalker.game.jinhua.domain.enums.RoomCardOperationEnum;
import cn.worldwalker.game.jinhua.domain.game.RoomCardOperationFailInfo;
import cn.worldwalker.game.jinhua.domain.result.Result;
import cn.worldwalker.game.jinhua.domain.result.ResultCode;
import cn.worldwalker.game.jinhua.service.game.TransactionService;
import cn.worldwalker.game.jinhua.service.game.impl.CommonService;
import cn.worldwalker.game.jinhua.service.session.SessionContainer;


public class RoomCardOperationFailProcessJob extends SingleServerJobByRedis {
	
	private final static Log log = LogFactory.getLog(RoomCardOperationFailProcessJob.class);
	
	@Autowired
	private TransactionService transactionService;
	@Autowired
	private CommonService commonService;
	
	@Override
	public void execute() {
		Result result = new Result();
		result.setMsgType(MsgTypeEnum.roomCardNumUpdate.msgType);
		Map<String, Object> data = new HashMap<String, Object>();
		result.setData(data);
		boolean flag = true;
		RoomCardOperationFailInfo failInfo = null;
		String failInfoStr = null;
		Result re = null;
		do {
			failInfoStr = jedisTemplate.rpop(Constant.jinhuaRoomCardOperationFailList);
			flag = StringUtils.isNotBlank(failInfoStr);
			if (flag) {
				failInfo = JsonUtil.toObject(failInfoStr, RoomCardOperationFailInfo.class);
				if (RoomCardOperationEnum.consumeCard.type.equals(failInfo.getRoomCardOperationType())) {
					try {
						re = transactionService.doDeductRoomCard(failInfo.getPlayerId(), failInfo.getPayType(), failInfo.getTotalGames(), RoomCardOperationEnum.jobCompensateConsumeCard);
						if (ResultCode.SUCCESS.code == re.getCode()) {
							data.put("playerId", failInfo.getPlayerId());
							data.put("roomCardNum", (Integer)re.getData());
							SessionContainer.sendTextMsgByPlayerId(failInfo.getPlayerId(), result);
						}else{/**如果扣除失败则重新加入redis*/
							commonService.addRoomCardOperationFailInfoToRedis(new RoomCardOperationFailInfo( failInfo.getPlayerId(), 
																											 failInfo.getPayType(), 
																											 failInfo.getTotalGames(), 
																							RoomCardOperationEnum.consumeCard.type));
						}
					} catch (Exception e) {
						log.error(ResultCode.ROOM_CARD_DEDUCT_EXCEPTION.returnDesc 
								 + ", playerId:" + failInfo.getPlayerId() 
								 + ", payType:" + failInfo.getPayType() 
								 + ", totalGames:" + failInfo.getTotalGames(), e);
						commonService.addRoomCardOperationFailInfoToRedis(new RoomCardOperationFailInfo( failInfo.getPlayerId(), 
								 failInfo.getPayType(), 
								 failInfo.getTotalGames(), 
								 RoomCardOperationEnum.consumeCard.type));
						break;
					}
				}
			}
		} while (flag);
	}

}

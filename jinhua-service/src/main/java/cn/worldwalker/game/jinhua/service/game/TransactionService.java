package cn.worldwalker.game.jinhua.service.game;

import cn.worldwalker.game.jinhua.domain.enums.RoomCardOperationEnum;
import cn.worldwalker.game.jinhua.domain.result.Result;

public interface TransactionService {
	public Result doDeductRoomCard(Long playerId, Integer payType, Integer totalGames, RoomCardOperationEnum operationEnum);
}

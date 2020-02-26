package logbook.gui.logic;

import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;

import logbook.data.context.GlobalContext;
import logbook.dto.ItemDto;
import logbook.dto.ShipDto;

/**
 * @author Nishikuma
 *
 */
public class FleetAnalysis {
    /**
     * 艦隊分析の艦娘フォーマットを取得します
     *
     * @param isLockedOnly ロックしている艦娘限定にするか
     * @return フォーマット
     */
    public String getShipsFormat(boolean isLockedOnly) {
        List<ShipDto> ships = GlobalContext.getShipMap().values().stream().collect(Collectors.toList());
        List<ShipDto> lockedShips = isLockedOnly ? ships.stream().filter(ShipDto::getLocked).collect(Collectors.toList()) : ships;

        return "[" + lockedShips.stream().map(ship -> {
            JsonObject json = ship.getJson();

            return Json.createObjectBuilder()
                .add("api_ship_id", json.getInt("api_ship_id"))
                .add("api_lv", json.getInt("api_lv"))
                .add("api_kyouka", json.getJsonArray("api_kyouka"))
                .add("api_exp", json.getJsonArray("api_exp")).build().toString();
        }).collect(Collectors.joining(",")) + "]";
    }
    /**
     * 艦隊分析の装備フォーマットを取得します
     *
     * @param isLockedOnly ロックしている装備限定にするか
     * @return フォーマット
     */
    public String getItemsFormat(boolean isLockedOnly) {
        List<ItemDto> items = GlobalContext.getItemMap().values().stream().collect(Collectors.toList());
        List<ItemDto> lockedItems = isLockedOnly ? items.stream().filter(ItemDto::isLocked).collect(Collectors.toList()) : items;

        return "[" + lockedItems.stream().map(item -> {
            return Json.createObjectBuilder()
                .add("api_slotitem_id", item.getSlotitemId())
                .add("api_level", item.getLevel()).build().toString();
        }).collect(Collectors.joining(",")) + "]";
    }
}

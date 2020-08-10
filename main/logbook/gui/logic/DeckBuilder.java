package logbook.gui.logic;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import logbook.data.context.GlobalContext;
import logbook.dto.AirbaseDto;
import logbook.dto.ItemDto;
import logbook.dto.ShipDto;
import logbook.dto.AirbaseDto.AirCorpsDto;
import logbook.dto.AirbaseDto.AirCorpsDto.SquadronDto;
import logbook.util.JsonUtils;

/**
 * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを作成するクラス
 *
 * @author Nishisonic
 */
public class DeckBuilder {
    /**
     * 艦隊シミュレーター＆デッキビルダーのフォーマットバージョン
     */
    public final String DECKBUILDER_FORMAT_VERSION = "4";

    /**
     * 制空権シミュレータのフォーマットバージョン
     */
    public final String KC_TOOLS_BUILDER_FORMAT_VERSION = "4.2";

    /**
     * 艦隊シミュレーター＆デッキビルダーのURL
     */
    public final String DECKBUILDER_URL = "http://kancolle-calc.net/deckbuilder.html";

    /**
     * 制空権シミュレータのURL
     */
    public final String KC_TOOLS_BUILDER_URL = "https://noro6.github.io/kcTools/";

    /**
     * 艦隊シミュレーター＆デッキビルダーのURLにつける語尾
     */
    public final String SUFFIX = "?predeck=";

    /**
     * 艦隊シミュレーター＆デッキビルダーのフォーマットバージョンを返します
     *
     * @return fORMAT_VERSION
     */
    public String getDeckbuilderFormatVersion() {
        return this.DECKBUILDER_FORMAT_VERSION;
    }

    /**
     * 制空権シミュレータのフォーマットバージョンを返します
     *
     * @return fORMAT_VERSION
     */
    public String getKcToolsBuilderFormatVersion() {
        return this.KC_TOOLS_BUILDER_FORMAT_VERSION;
    }

    /**
     * 艦隊シミュレーター＆デッキビルダーのURLを返します
     *
     * @return URL
     */
    public String getDeckbuilderURL() {
        return this.DECKBUILDER_URL;
    }

    /**
     * 制空権シミュレータのURLを返します
     *
     * @return URL
     */
    public String getKcToolsBuilderURL() {
        return this.KC_TOOLS_BUILDER_URL;
    }

    /**
     * 艦隊シミュレーター＆デッキビルダーのURLにつける語尾を返します
     *
     * @return Suffix
     */
    public String getSuffix() {
        return this.SUFFIX;
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param needsUsedDock どの艦隊のデータを用いるか[第一艦隊,第二艦隊,第三艦隊,第四艦隊]
     * @return format フォーマット
     */
    public String getDeckBuilderFormat(boolean[] needsUsedDock) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", this.DECKBUILDER_FORMAT_VERSION);
        try {
            IntStream.rangeClosed(1, GlobalContext.getBasicInfo().getDeckCount())
                    .filter(dockId -> needsUsedDock[dockId - 1])
                    .boxed()
                    .collect(Collectors.toMap(dockId -> dockId,
                            dockId -> GlobalContext.getDock(dockId.toString()).getShips()))
                    .forEach((dockId, ships) -> {
                        JsonObjectBuilder fleet = Json.createObjectBuilder();

                        IntStream.range(0, ships.size()).forEach(shipIdx -> {
                            JsonObjectBuilder ship = Json.createObjectBuilder();
                            ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                            ship.add("lv", ships.get(shipIdx).getLv());
                            ship.add("luck", ships.get(shipIdx).getLucky());
                            JsonObjectBuilder items = Json.createObjectBuilder();
                            List<ItemDto> item2 = ships.get(shipIdx).getItem2();
                            int slotNum = ships.get(shipIdx).getSlotNum();

                            IntStream.range(0, slotNum)
                                    .filter(itemIdx -> Optional.ofNullable(item2.get(itemIdx)).isPresent())
                                    .boxed()
                                    .collect(Collectors.toMap(itemIdx -> itemIdx, itemIdx -> item2.get(itemIdx)))
                                    .forEach((itemIdx, itemDto) -> {
                                        JsonObjectBuilder item = Json.createObjectBuilder();
                                        item.add("id", item2.get(itemIdx).getSlotitemId());
                                        if (item2.get(itemIdx).getLevel() > 0) {
                                            item.add("rf", Integer.toString(item2.get(itemIdx).getLevel()));
                                        }
                                        else {
                                            item.add("rf", 0);
                                        }
                                        item.add("mas", Integer.toString(item2.get(itemIdx).getAlv()));
                                        items.add("i" + (itemIdx + 1), item);
                                    });

                            Optional.ofNullable(ships.get(shipIdx).getSlotExItem()).ifPresent(slotExItem -> {
                                JsonObjectBuilder item = Json.createObjectBuilder();
                                item.add("id", slotExItem.getSlotitemId());
                                item.add("rf", slotExItem.getLevel());
                                item.add("mas", slotExItem.getAlv());
                                if (slotNum < 5) {
                                    items.add("i" + (slotNum + 1), item);
                                }
                                else {
                                    items.add("ix", item);
                                }
                            });
                            ship.add("items", items);

                            fleet.add("s" + (shipIdx + 1), ship);
                        });
                        deck.add("f" + dockId, fleet);
                    });
            return deck.build().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param fleets 艦隊
     * @return format フォーマット
     */
    @SuppressWarnings("unchecked")
    public String getDeckBuilderFormat(List<ShipDto>... fleets) {
        JsonObjectBuilder deck = Json.createObjectBuilder();
        deck.add("version", this.DECKBUILDER_FORMAT_VERSION);
        try {
            IntStream.rangeClosed(1, fleets.length)
                    .boxed()
                    .collect(Collectors.toMap(dockId -> dockId, dockId -> fleets[dockId - 1]))
                    .forEach((dockId, ships) -> {
                        JsonObjectBuilder fleet = Json.createObjectBuilder();

                        IntStream.range(0, ships.size()).forEach(shipIdx -> {
                            JsonObjectBuilder ship = Json.createObjectBuilder();
                            ship.add("id", Integer.toString(ships.get(shipIdx).getShipInfo().getShipId()));
                            ship.add("lv", ships.get(shipIdx).getLv());
                            ship.add("luck", ships.get(shipIdx).getLucky());
                            JsonObjectBuilder items = Json.createObjectBuilder();
                            List<ItemDto> item2 = ships.get(shipIdx).getItem2();

                            IntStream.range(0, item2.size())
                                    .filter(itemIdx -> Optional.ofNullable(item2.get(itemIdx)).isPresent())
                                    .boxed()
                                    .collect(Collectors.toMap(itemIdx -> itemIdx, itemIdx -> item2.get(itemIdx)))
                                    .forEach((itemIdx, itemDto) -> {
                                        JsonObjectBuilder item = Json.createObjectBuilder();
                                        item.add("id", item2.get(itemIdx).getSlotitemId());
                                        if (item2.get(itemIdx).getLevel() > 0) {
                                            item.add("rf", Integer.toString(item2.get(itemIdx).getLevel()));
                                        }
                                        else {
                                            item.add("rf", 0);
                                        }
                                        item.add("mas", Integer.toString(item2.get(itemIdx).getAlv()));
                                        items.add("i" + (itemIdx + 1), item);
                                    });

                            Optional.ofNullable(ships.get(shipIdx).getSlotExItem()).ifPresent(slotExItem -> {
                                JsonObjectBuilder item = Json.createObjectBuilder();
                                item.add("id", slotExItem.getSlotitemId());
                                if (slotExItem.getLevel() > 0) {
                                    item.add("rf", Integer.toString(slotExItem.getLevel()));
                                }
                                else {
                                    item.add("rf", 0);
                                }
                                item.add("mas", Integer.toString(slotExItem.getAlv()));
                                int slotNum = ships.get(shipIdx).getSlotNum();
                                if (slotNum < 5) {
                                    items.add("i" + (slotNum + 1), item);
                                }
                                else {
                                    items.add("ix", item);
                                }
                            });
                            ship.add("items", items);

                            fleet.add("s" + (shipIdx + 1), ship);
                        });
                        deck.add("f" + dockId, fleet);
                    });
            return deck.build().toString();
        } catch (NullPointerException e) {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのフォーマットを返します(全艦隊)
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @return format フォーマット
     */
    public String getDeckBuilderFormat() {
        boolean[] b = { true, true, true, true };
        return this.getDeckBuilderFormat(b);
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param fleets 艦隊
     * @return url URL
     */
    @SuppressWarnings("unchecked")
    public String getDeckBuilderURL(List<ShipDto>... fleets) {
        Optional<String> formatOpt = Optional.ofNullable(this.getDeckBuilderFormat(fleets));
        if (formatOpt.isPresent()) {
            return this.DECKBUILDER_URL + this.SUFFIX + formatOpt.get();
        }
        else {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @param needsUsedDock どの艦隊のデータを用いるか[第一艦隊,第二艦隊,第三艦隊,第四艦隊]
     * @return url URL
     */
    public String getDeckBuilderURL(boolean[] needsUsedDock) {
        Optional<String> formatOpt = Optional.ofNullable(this.getDeckBuilderFormat(needsUsedDock));
        if (formatOpt.isPresent()) {
            return this.DECKBUILDER_URL + this.SUFFIX + formatOpt.get();
        }
        else {
            return null;
        }
    }

    /**
     * 艦載機厨氏の艦隊シミュレーター＆デッキビルダーのURLを作成します(全艦隊)
     * ただし、データが出揃っていない場合はnullが返されます
     *
     * @return url URL
     */
    public String getDeckBuilderURL() {
        boolean[] b = { true, true, true, true };
        return this.getDeckBuilderURL(b);
    }

    private static String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String getKcToolsBuilderURL(boolean[] needsUsedDock, boolean isEvent) {
        Optional<String> formatOpt = Optional.ofNullable(this.getKcToolsBuilderFormat(needsUsedDock, isEvent));
        if (formatOpt.isPresent()) {
            return this.KC_TOOLS_BUILDER_URL + this.SUFFIX + formatOpt.get();
        }
        else {
            return null;
        }
    }

    public String getKcToolsBuilderFormat(boolean[] needsUsedDock, boolean isEvent) {
        JsonObject deck = JsonUtils.fromString(this.getDeckBuilderFormat(needsUsedDock));
        JsonObjectBuilder json = Json.createObjectBuilder();
        json.add("version", this.KC_TOOLS_BUILDER_FORMAT_VERSION);
        for (int i = 1; i <= 4; i++) {
            if (deck.containsKey("f" + i)) {
                json.add("f" + i, deck.getJsonObject("f" + i));
            }
        }
        Optional.ofNullable(GlobalContext.getAirbase()).ifPresent(airbases -> {
            Map<Integer, Map<Integer, AirCorpsDto>> airbase = airbases.get();
            int area = isEvent ? airbase.keySet().stream().filter(a -> a >= 22).findFirst().orElse(-1)
                    : airbase.containsKey(6) ? 6 : -1;
            if (area > 0) {
                airbase.get(area).forEach((id, aircorps) -> {
                    Map<Integer, SquadronDto> squadrons = aircorps.getSquadrons();
                    JsonObjectBuilder squadronJson = Json.createObjectBuilder();
                    squadrons.forEach((i, squadron) -> {
                        ItemDto item = GlobalContext.getItem(squadron.getSlotid());
                        if (Objects.nonNull(item)) {
                            squadronJson.add("i" + i, Json.createObjectBuilder()
                                    .add("id", item.getSlotitemId())
                                    .add("rf", item.getLevel())
                                    .add("mas", item.getAlv()));
                        }
                    });
                    json.add("a" + id, Json.createObjectBuilder()
                            .add("mode", 1)
                            .add("items", squadronJson));
                });
            }
        });
        return json.build().toString();
    }
}

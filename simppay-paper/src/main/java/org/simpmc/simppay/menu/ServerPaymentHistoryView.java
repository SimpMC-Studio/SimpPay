package org.simpmc.simppay.menu;

import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.ViewConfigBuilder;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.simpmc.simppay.SPPlugin;
import org.simpmc.simppay.config.ConfigManager;
import org.simpmc.simppay.config.types.data.menu.DisplayItem;
import org.simpmc.simppay.config.types.data.menu.RoleType;
import org.simpmc.simppay.config.types.menu.ServerPaymentHistoryMenuConfig;
import org.simpmc.simppay.data.PaymentType;
import org.simpmc.simppay.database.dto.PaymentRecord;
import org.simpmc.simppay.util.CalendarUtil;
import org.simpmc.simppay.util.MessageUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServerPaymentHistoryView extends View {


    private final State<Pagination> paginationState = buildComputedAsyncPaginationState(this::fetchPaymentRecordsAsync)
            .elementFactory((ctx, bukkitItemComponentBuilder, i, paymentRecord) -> {
                ServerPaymentHistoryMenuConfig config = ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class);
                MessageUtil.debug(paymentRecord.toString());
                if (paymentRecord.getPaymentType() == PaymentType.CARD) {
                    DisplayItem item = config.cardItem.clone().replaceStringInName("{amount}", value -> {
                        String formattedValue = String.format("%,.0f", paymentRecord.getAmount());
                        return formattedValue + "đ";
                    }).replaceStringInName("{card_type}", paymentRecord.getTelco());
                    List<String> lores = item.getLores().stream()
                            .map(line ->
                                    line.replace("{time}", CalendarUtil.getFormattedTimestamp(paymentRecord.getTimestamp().getTime()))
                                            .replace("{serial}", paymentRecord.getSerial().orElse("0"))
                                            .replace("{pin}", paymentRecord.getPin().orElse("0"))
                                            .replace("{api}", paymentRecord.getProvider())
                                            .replace("{transaction_id}", paymentRecord.getRefId())
                                            .replace("{name}", paymentRecord.getPlayerName()
                                            ))
                            .toList();
                    item.setLores(lores);
                    bukkitItemComponentBuilder.withItem(item.getItemStack(ctx.getPlayer()));
                }

                if (paymentRecord.getPaymentType() == PaymentType.BANKING) {
                    DisplayItem item = config.bankItem.clone().replaceStringInName("{amount}", value -> {
                                String formattedValue = String.format("%,.0f", paymentRecord.getAmount());
                                return formattedValue + "đ";
                            })
                            .replaceStringInName("{card_type}", paymentRecord.getTelco());
                    List<String> lores = item.getLores().stream()
                            .map(line ->
                                    line.replace("{time}", CalendarUtil.getFormattedTimestamp(paymentRecord.getTimestamp().getTime()))
                                            .replace("{api}", paymentRecord.getProvider())
                                            .replace("{transaction_id}", paymentRecord.getRefId())
                                            .replace("{name}", paymentRecord.getPlayerName()
                                            ))
                            .toList();
                    item.setLores(lores);
                    bukkitItemComponentBuilder.withItem(item.getItemStack(ctx.getPlayer()));
                }
            })
            // TODO: inventory-framework API is broken for this, it is substring the title down to 32 letters, not sure why
            // Paper also noted that the practice is poorly written for InventoryView#setTitle() which this calls anyways above 1.20 :
            // i love and hate paper at the same time
//            .onPageSwitch((ctx, pagination) -> {
//                ServerPaymentHistoryMenuConfig config = ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class);
//                // wtf shit
//                String title = LegacyComponentSerializer.legacySection().serializeOr(
//                        MessageUtil.getComponentParsed(
//                                config.title.replace("{page}", String.valueOf(pagination.currentPage())), ctx.getPlayer()), "Null");
//                ctx.updateTitleForPlayer(title, ctx.getPlayer());
//            })
            .build();

    @Override
    public void onInit(ViewConfigBuilder config) {
        config.cancelInteractions();
        config.layout(ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class).layout.toArray(new String[0]));
        Component title = MessageUtil.getComponentParsed(ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class).title, null);
        config.title(title);
    }

//    @Override
//    public void onOpen(@NotNull OpenContext open) {
//        ServerPaymentHistoryMenuConfig menuConfig = ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class);
//        String title = menuConfig.title;
//        open.modifyConfig().title(MessageUtil.getComponentParsed(title, open.getPlayer())); // Title support papi
//    }

    @Override
    public void onFirstRender(@NotNull RenderContext render) {
        ServerPaymentHistoryMenuConfig menuConfig = ConfigManager.getInstance().getConfig(ServerPaymentHistoryMenuConfig.class);
        Pagination pagination = paginationState.get(render);

        Map<Character, DisplayItem> displayedItems = menuConfig.displayItems;

        for (Map.Entry<Character, DisplayItem> entry : displayedItems.entrySet()) {
            DisplayItem item = entry.getValue();
            if (item.getRole() == RoleType.NONE) {
                render.layoutSlot(entry.getKey(), entry.getValue().getItemStack());
            }
            if (item.getRole() == RoleType.PREV_PAGE) {
                render.layoutSlot(entry.getKey(), item.getItemStack())
                        .displayIf(() -> pagination.currentPageIndex() != 0)
                        .updateOnStateChange(paginationState)
                        .onClick((ctx) -> {
                            paginationState.get(ctx).back();
                        });
            }
            if (item.getRole() == RoleType.NEXT_PAGE) {
                render.layoutSlot(entry.getKey(), item.getItemStack())
                        .displayIf(() -> pagination.currentPageIndex() < pagination.lastPageIndex())
                        .updateOnStateChange(paginationState)
                        .onClick((ctx) -> {
                            paginationState.get(ctx).advance();
                        });
            }
        }
    }

    private CompletableFuture<List<PaymentRecord>> fetchPaymentRecordsAsync(Context context) {
        return CompletableFuture.supplyAsync(() -> {
            List<PaymentRecord> paymentRecords = SPPlugin.getInstance().getDatabaseService().getPaymentLogService().getEntireServerPayments();
            return paymentRecords;
        });
    }
}

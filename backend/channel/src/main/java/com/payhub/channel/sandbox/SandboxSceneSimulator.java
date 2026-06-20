package com.payhub.channel.sandbox;

import com.payhub.channel.dto.*;
import com.payhub.channel.enums.OrderStatusEnum;
import com.payhub.channel.enums.RefundStatusEnum;
import com.payhub.common.context.SandboxContext;
import com.payhub.common.enums.SandboxSceneEnum;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SandboxSceneSimulator {

    public static UnifiedOrderResponse simulateUnifiedOrder(UnifiedOrderRequest request,
                                                             java.util.function.Supplier<UnifiedOrderResponse> successSupplier) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 统一下单, scene={}, orderNo={}", scene.getCode(), request.getOrderNo());

        switch (scene) {
            case TIMEOUT:
                return simulateTimeout(request);
            case INSUFFICIENT_BALANCE:
                return simulateInsufficientBalance(request);
            case FAILED:
            case CHANNEL_ERROR:
                return simulateChannelError(request, scene);
            case SUCCESS:
            case REPEAT_NOTIFY:
            case SIGN_ERROR:
            case AMOUNT_MISMATCH:
            default:
                return successSupplier.get();
        }
    }

    public static QueryOrderResponse simulateQueryOrder(String orderNo, String channelTradeNo,
                                                         java.util.function.Supplier<QueryOrderResponse> successSupplier) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 查询订单, scene={}, orderNo={}", scene.getCode(), orderNo);

        if (scene == SandboxSceneEnum.FAILED || scene == SandboxSceneEnum.CHANNEL_ERROR) {
            return QueryOrderResponse.fail("FAIL", scene == SandboxSceneEnum.FAILED ? "支付失败" : "通道异常");
        }
        return successSupplier.get();
    }

    public static RefundResponse simulateRefund(RefundRequest request,
                                                 java.util.function.Supplier<RefundResponse> successSupplier) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 退款, scene={}, refundNo={}", scene.getCode(), request.getRefundNo());

        if (scene == SandboxSceneEnum.REFUND_FAILED || scene == SandboxSceneEnum.CHANNEL_ERROR) {
            return RefundResponse.fail("REFUND_FAIL", scene == SandboxSceneEnum.REFUND_FAILED ? "退款失败" : "通道异常");
        }
        return successSupplier.get();
    }

    public static NotifyResult simulateNotify(String notifyData, Map<String, String> params,
                                               java.util.function.Supplier<NotifyResult> successSupplier,
                                               java.math.BigDecimal originalAmount) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 回调通知, scene={}", scene.getCode());

        NotifyResult result = successSupplier.get();

        switch (scene) {
            case SIGN_ERROR:
                result.setPayStatus(OrderStatusEnum.FAILED.getCode());
                log.info("[沙箱场景模拟] 签名错误，标记支付失败");
                break;
            case AMOUNT_MISMATCH:
                BigDecimal mismatchedAmount = originalAmount.multiply(new BigDecimal("0.9"));
                result.setPayAmount(mismatchedAmount);
                log.info("[沙箱场景模拟] 金额不匹配，原金额:{}, 回调金额:{}", originalAmount, mismatchedAmount);
                break;
            case FAILED:
                result.setPayStatus(OrderStatusEnum.FAILED.getCode());
                break;
            default:
                break;
        }
        return result;
    }

    public static boolean shouldRepeatNotify() {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        return scene == SandboxSceneEnum.REPEAT_NOTIFY;
    }

    public static boolean verifyNotifyInSandbox(boolean defaultResult) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        if (scene == SandboxSceneEnum.SIGN_ERROR) {
            log.info("[沙箱场景模拟] 签名错误，验签失败");
            return false;
        }
        return defaultResult;
    }

    private static UnifiedOrderResponse simulateTimeout(UnifiedOrderRequest request) {
        try {
            log.info("[沙箱场景模拟] 模拟支付超时，sleep 3秒...");
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return UnifiedOrderResponse.fail("TIMEOUT", "支付超时，请稍后重试");
    }

    private static UnifiedOrderResponse simulateInsufficientBalance(UnifiedOrderRequest request) {
        return UnifiedOrderResponse.fail("INSUFFICIENT_BALANCE", "账户余额不足");
    }

    private static UnifiedOrderResponse simulateChannelError(UnifiedOrderRequest request, SandboxSceneEnum scene) {
        String errorMsg = scene == SandboxSceneEnum.FAILED ? "支付失败" : "通道系统异常，请稍后重试";
        String errorCode = scene == SandboxSceneEnum.FAILED ? "PAY_FAIL" : "SYSTEM_ERROR";
        return UnifiedOrderResponse.fail(errorCode, errorMsg);
    }

    public static BarcodePayResponse simulateBarcodePay(BarcodePayRequest request,
                                                          java.util.function.Supplier<BarcodePayResponse> successSupplier) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 条码支付(被扫), scene={}, orderNo={}", scene.getCode(), request.getOrderNo());

        switch (scene) {
            case TIMEOUT:
                try {
                    log.info("[沙箱场景模拟] 模拟条码支付超时，sleep 3秒...");
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return BarcodePayResponse.fail("TIMEOUT", "条码支付超时，请稍后重试");
            case INSUFFICIENT_BALANCE:
                return BarcodePayResponse.fail("INSUFFICIENT_BALANCE", "用户账户余额不足");
            case FAILED:
            case CHANNEL_ERROR:
                return BarcodePayResponse.fail("PAY_FAIL",
                        scene == SandboxSceneEnum.FAILED ? "条码支付失败" : "通道系统异常，请稍后重试");
            case SUCCESS:
            case REPEAT_NOTIFY:
            case SIGN_ERROR:
            case AMOUNT_MISMATCH:
            default:
                return successSupplier.get();
        }
    }

    public static FacePayResponse simulateFacePay(FacePayRequest request,
                                                     java.util.function.Supplier<FacePayResponse> successSupplier) {
        SandboxSceneEnum scene = SandboxSceneEnum.getByCode(SandboxContext.getScene());
        log.info("[沙箱场景模拟] 刷脸支付, scene={}, orderNo={}", scene.getCode(), request.getOrderNo());

        switch (scene) {
            case TIMEOUT:
                try {
                    log.info("[沙箱场景模拟] 模拟刷脸支付超时，sleep 3秒...");
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return FacePayResponse.fail("TIMEOUT", "刷脸支付超时，请稍后重试");
            case INSUFFICIENT_BALANCE:
                return FacePayResponse.fail("INSUFFICIENT_BALANCE", "用户账户余额不足");
            case FAILED:
            case CHANNEL_ERROR:
                return FacePayResponse.fail("FACE_FAIL",
                        scene == SandboxSceneEnum.FAILED ? "刷脸支付失败" : "通道系统异常，请稍后重试");
            case SUCCESS:
            case REPEAT_NOTIFY:
            case SIGN_ERROR:
            case AMOUNT_MISMATCH:
            default:
                return successSupplier.get();
        }
    }
}

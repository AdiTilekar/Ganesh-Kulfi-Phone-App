package com.ganeshkulfi.app.data.model

data class Payment(
    val id: String = "",
    val retailerId: String = "",
    val amount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentGateway: PaymentGateway = PaymentGateway.NONE,
    val transactionId: String = "",
    val status: PaymentStatusEnum = PaymentStatusEnum.PENDING,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

enum class PaymentMethod {
    CASH,
    UPI,
    CARD,
    NET_BANKING,
    CHEQUE,
    RAZORPAY,
    PAYTM,
    PHONEPE,
    GPAY
}

enum class PaymentGateway {
    NONE,           // For cash/cheque
    RAZORPAY,       // Integration ready - to be implemented later
    PAYTM,
    PHONEPE,
    STRIPE,
    CASHFREE
}

enum class PaymentStatusEnum {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    REFUNDED
}

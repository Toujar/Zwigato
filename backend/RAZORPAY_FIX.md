# Razorpay Configuration Fix

## 🔴 Problem

Application was crashing during startup with:

```
Caused by: java.lang.IllegalStateException: Razorpay credentials not configured.
at com.fooddelivery.config.RazorpayConfig.razorpayClient(RazorpayConfig.java:39)
```

The `RazorpayConfig` bean was being created unconditionally and throwing an exception when credentials were missing.

## ✅ Solution

Made Razorpay integration **conditional** and **optional**:

1. **RazorpayClient bean only created when enabled**
2. **Graceful error handling in PaymentService**
3. **Application starts successfully without Razorpay credentials**

## 📝 Changes Made

### 1. RazorpayConfig.java

**Added:**
- `@ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")` on the bean
- `razorpay.enabled` boolean field
- `isConfigured()` helper method to check if Razorpay is ready

**Result:**
```java
@Bean
@ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
public RazorpayClient razorpayClient() throws Exception {
    // Only created when razorpay.enabled=true
}

public boolean isConfigured() {
    return enabled 
        && keyId != null && !keyId.isBlank() 
        && keySecret != null && !keySecret.isBlank();
}
```

### 2. application.properties

**Added:**
```properties
# Set to true to enable Razorpay integration
# When false, the RazorpayClient bean is not created
razorpay.enabled=${RAZORPAY_ENABLED:false}

razorpay.key_id=${RAZORPAY_KEY_ID:}
razorpay.key_secret=${RAZORPAY_KEY_SECRET:}
```

**Default behavior:**
- `razorpay.enabled` defaults to `false`
- Application starts without Razorpay
- RazorpayClient bean is **not created**

### 3. PaymentServiceImpl.java

**Added checks before using RazorpayClient:**

```java
// Check if Razorpay is configured
if (!razorpayConfig.isConfigured()) {
    throw new BadRequestException(
        "Payment gateway is not configured. Please contact support.");
}

RazorpayClient razorpayClient = razorpayConfig.razorpayClient();
```

**Applied to:**
- ✅ `createPayment()` - order payment initiation
- ✅ `refundPayment()` - refund processing
- ✅ `checkRefundStatus()` - refund status polling

## 🚀 Deployment Options

### Option 1: Deploy WITHOUT Razorpay (Recommended for Initial Deployment)

**No action needed!** The application will start successfully with:
- `RAZORPAY_ENABLED=false` (default)
- No credentials required

**API behavior:**
- Payment endpoints return error: `"Payment gateway is not configured"`
- All other endpoints work normally
- Application is fully functional except payment processing

### Option 2: Deploy WITH Razorpay Test Credentials

**Add to Render environment variables:**

```bash
RAZORPAY_ENABLED=true
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
```

**Where to get test credentials:**
1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com/)
2. Switch to **Test Mode** (important!)
3. Navigate to **Settings → API Keys**
4. Generate or copy:
   - **Key ID** (starts with `rzp_test_`)
   - **Key Secret**

**Test mode:**
- ✅ No real money charged
- ✅ Test cards work (e.g., 4111 1111 1111 1111)
- ✅ Full payment flow testing
- ✅ Safe for development/staging

### Option 3: Deploy WITH Razorpay Live Credentials (Production Only)

**⚠️ Only for production with real payments!**

```bash
RAZORPAY_ENABLED=true
RAZORPAY_KEY_ID=rzp_live_xxxxxxxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxxxxxxxxxx
```

## 📊 Configuration Matrix

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `RAZORPAY_ENABLED` | `false` | Enable/disable Razorpay integration |
| `RAZORPAY_KEY_ID` | `""` | Razorpay API Key ID |
| `RAZORPAY_KEY_SECRET` | `""` | Razorpay API Secret |

## 🔍 How It Works

### Without Razorpay (RAZORPAY_ENABLED=false or missing)

```
Application starts
↓
RazorpayConfig loads
↓
@ConditionalOnProperty checks razorpay.enabled
↓
razorpay.enabled = false
↓
RazorpayClient bean NOT created ✅
↓
Application fully starts ✅
↓
Payment endpoints called
↓
razorpayConfig.isConfigured() returns false
↓
Returns error: "Payment gateway is not configured" ✅
```

### With Razorpay (RAZORPAY_ENABLED=true + credentials)

```
Application starts
↓
RazorpayConfig loads
↓
@ConditionalOnProperty checks razorpay.enabled
↓
razorpay.enabled = true
↓
Credentials validated
↓
RazorpayClient bean created ✅
↓
Application fully starts ✅
↓
Payment endpoints called
↓
razorpayConfig.isConfigured() returns true
↓
Razorpay API integration works ✅
```

## 🎯 Recommended Approach for Render Deployment

### Phase 1: Deploy Without Razorpay

1. **No environment variables needed** (defaults work)
2. Push code to GitHub
3. Render auto-deploys
4. Application starts successfully ✅
5. Test all non-payment endpoints

### Phase 2: Enable Razorpay Test Mode

1. Get Razorpay **test** credentials
2. Add to Render environment:
   ```
   RAZORPAY_ENABLED=true
   RAZORPAY_KEY_ID=rzp_test_...
   RAZORPAY_KEY_SECRET=...
   ```
3. Render auto-redeploys
4. Test payment flow with test cards

### Phase 3: Production (Later)

1. Get Razorpay **live** credentials
2. Update Render environment
3. Test with small real transaction
4. Monitor logs and transactions

## 🛡️ Benefits of This Approach

✅ **Graceful degradation** - app works without payment gateway  
✅ **No crashes on startup** - conditional bean creation  
✅ **Clear error messages** - users know payment is unavailable  
✅ **Easy to enable later** - just set environment variables  
✅ **Safe defaults** - disabled by default  
✅ **Test mode support** - use test credentials safely  
✅ **Production ready** - same code works in all environments  

## 🔧 Testing Locally

### Without Razorpay
```bash
# application.properties or environment
RAZORPAY_ENABLED=false
# or just leave it unset (defaults to false)

# Start application
./mvnw spring-boot:run

# Application starts ✅
```

### With Razorpay Test Mode
```bash
# application.properties or environment
RAZORPAY_ENABLED=true
RAZORPAY_KEY_ID=rzp_test_xxxxx
RAZORPAY_KEY_SECRET=xxxxx

# Start application
./mvnw spring-boot:run

# Application starts ✅
# Payment endpoints work ✅
```

## 📞 Next Steps

1. ✅ Code changes committed
2. 🔄 Push to GitHub
3. 🔄 Render auto-deploys
4. ✅ Application should start successfully **without Razorpay credentials**
5. 🎯 Later: Add test credentials to enable payment testing

The application will now start successfully on Render without requiring Razorpay credentials!

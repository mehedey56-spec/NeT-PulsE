package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.data.NetPulseDatabase
import com.example.data.NetPulseRepository
import com.example.ui.components.NetPulseBottomNav
import com.example.ui.components.NetPulseHeader
import com.example.ui.screens.*
import com.example.ui.theme.NetPulseTheme
import com.example.ui.viewmodel.NetPulseScreen
import com.example.ui.viewmodel.NetPulseViewModel
import com.example.ui.viewmodel.NetPulseViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: NetPulseViewModel by viewModels {
        val database = NetPulseDatabase.getDatabase(applicationContext)
        val repository = NetPulseRepository(database)
        NetPulseViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NetPulseTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()
                val telemetry by viewModel.telemetry.collectAsState()
                val routerConfig by viewModel.routerConfig.collectAsState()
                val devices by viewModel.filteredDevices.collectAsState()
                val adminGateState by viewModel.adminGateState.collectAsState()
                val searchQuery by viewModel.searchQuery.collectAsState()
                val selectedFilter by viewModel.selectedFilter.collectAsState()
                val currentUser by viewModel.currentUser.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        NetPulseHeader(
                            gatewayIp = routerConfig.gatewayIp,
                            isAdminUnlocked = adminGateState.isUnlocked,
                            username = currentUser?.username,
                            isLiveMode = routerConfig.isLiveMode,
                            onDemoBannerClick = { viewModel.navigateTo(NetPulseScreen.SETTINGS) },
                            onAdminLockClick = { viewModel.navigateTo(NetPulseScreen.ADMIN) },
                            onAuthClick = { viewModel.navigateTo(NetPulseScreen.AUTH) }
                        )
                    },
                    bottomBar = {
                        NetPulseBottomNav(
                            currentScreen = currentScreen,
                            onNavigate = { viewModel.navigateTo(it) }
                        )
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            NetPulseScreen.DASHBOARD -> DashboardScreen(
                                telemetry = telemetry,
                                routerConfig = routerConfig,
                                devices = devices,
                                onToggleBlock = { dev -> viewModel.toggleBlockDevice(dev) },
                                onSetSpeedLimit = { dev, limit -> viewModel.setDeviceSpeedLimit(dev, limit) },
                                onBlockAllUnknown = { cb -> viewModel.blockAllUnknownDevices(cb) },
                                onNavigateToDevices = { viewModel.navigateTo(NetPulseScreen.DEVICES) },
                                onNavigateToAdmin = { viewModel.navigateTo(NetPulseScreen.ADMIN) },
                                onNavigateToSettings = { viewModel.navigateTo(NetPulseScreen.SETTINGS) }
                            )
                            NetPulseScreen.DEVICES -> DevicesScreen(
                                devices = devices,
                                searchQuery = searchQuery,
                                selectedFilter = selectedFilter,
                                onSearchQueryChanged = { query -> viewModel.setSearchQuery(query) },
                                onFilterSelected = { filter -> viewModel.setDeviceFilter(filter) },
                                onToggleBlock = { dev -> viewModel.toggleBlockDevice(dev) },
                                onSetSpeedLimit = { dev, limit -> viewModel.setDeviceSpeedLimit(dev, limit) },
                                onBlockAllUnknown = { cb -> viewModel.blockAllUnknownDevices(cb) },
                                onAddDevice = { name, ip, mac, vendor, conn ->
                                    viewModel.addCustomDevice(name, ip, mac, vendor, conn)
                                }
                            )
                            NetPulseScreen.ADMIN -> AdminGateScreen(
                                adminState = adminGateState,
                                devices = devices,
                                onPasscodeChanged = { viewModel.updateAdminPasscode(it) },
                                onVerifyPasscode = { viewModel.verifyAdminPasscode() },
                                onLockAdmin = { viewModel.lockAdmin() },
                                onToggleBlock = { dev -> viewModel.toggleBlockDevice(dev) },
                                onUnblockAll = { viewModel.unblockAllDevices() }
                            )
                            NetPulseScreen.SETTINGS -> SettingsScreen(
                                routerConfig = routerConfig,
                                onSaveConfig = { gateway, user, pass, isLive ->
                                    viewModel.saveRouterConfig(gateway, user, pass, isLive)
                                },
                                onResetDatabase = { viewModel.resetDatabaseToDefaults() }
                            )
                            NetPulseScreen.AUTH -> AuthScreen(
                                currentUser = currentUser,
                                onLogin = { user, pass -> viewModel.loginUser(user, pass) },
                                onRegister = { user, pass -> viewModel.registerUser(user, pass) },
                                onLogout = { viewModel.logoutUser() }
                            )
                        }
                    }
                }
            }
        }
    }
}
package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val macAddress: String,
    val name: String,
    val ipAddress: String,
    val vendor: String,
    val isBlocked: Boolean = false,
    val signalStrength: Int = -55, // dBm
    val downloadKbps: Double = 0.0,
    val uploadKbps: Double = 0.0,
    val connectionType: String = "5 GHz", // "5 GHz", "2.4 GHz", "Ethernet"
    val speedLimitMbps: Double = 0.0, // 0.0 = Unlimited, >0 = Throttled
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "router_config")
data class RouterConfigEntity(
    @PrimaryKey val id: Int = 1,
    val gatewayIp: String = "192.168.1.1",
    val adminUsername: String = "admin",
    val adminPasswordHash: String = "admin",
    val routerBrand: String = "TP-Link Archer AX55",
    val isLiveMode: Boolean = false // false = Demo Mode, true = Live Router Scan Mode
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val username: String,
    val passwordHash: String,
    val role: String = "ADMIN",
    val createdTime: Long = System.currentTimeMillis()
)

data class LiveTelemetry(
    val pingMs: Int = 14,
    val liveDownloadMbps: Double = 142.5,
    val liveUploadMbps: Double = 28.4,
    val externalIp: String = "182.168.42.109",
    val downloadHistory: List<Float> = listOf(120f, 135f, 110f, 142f, 150f, 138f, 145f, 142.5f),
    val uploadHistory: List<Float> = listOf(22f, 25f, 28f, 24f, 30f, 29f, 27f, 28.4f)
)package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY lastSeen DESC")
    fun getAllDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: DeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllDevices(devices: List<DeviceEntity>)

    @Query("UPDATE devices SET isBlocked = :blocked WHERE macAddress = :mac")
    suspend fun setDeviceBlocked(mac: String, blocked: Boolean)

    @Query("UPDATE devices SET speedLimitMbps = :speedLimit WHERE macAddress = :mac")
    suspend fun setSpeedLimit(mac: String, speedLimit: Double)

    @Query("UPDATE devices SET isBlocked = 1 WHERE vendor LIKE '%Generic%' OR vendor LIKE '%Unknown%' OR name LIKE '%Unknown%' OR name LIKE '%Generic%'")
    suspend fun blockAllUnknownDevices(): Int

    @Query("UPDATE devices SET isBlocked = 0")
    suspend fun unblockAllDevices()

    @Query("DELETE FROM devices WHERE macAddress = :mac")
    suspend fun deleteDevice(mac: String)

    @Query("DELETE FROM devices")
    suspend fun deleteAllDevices()
}

@Dao
interface RouterConfigDao {
    @Query("SELECT * FROM router_config WHERE id = 1 LIMIT 1")
    fun getRouterConfig(): Flow<RouterConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRouterConfig(config: RouterConfigEntity)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
}package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceEntity::class, RouterConfigEntity::class, UserEntity::class],
    version = 2,
    exportSchema = false
)
abstract class NetPulseDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun routerConfigDao(): RouterConfigDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: NetPulseDatabase? = null

        fun getDatabase(context: Context): NetPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NetPulseDatabase::class.java,
                    "netpulse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun NetPulseHeader(
    gatewayIp: String,
    isAdminUnlocked: Boolean,
    username: String?,
    isLiveMode: Boolean = false,
    onDemoBannerClick: () -> Unit = {},
    onAdminLockClick: () -> Unit,
    onAuthClick: () -> Unit
) {
    Surface(
        color = DeepObsidianBg,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (!isLiveMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StatusWarning.copy(alpha = 0.18f))
                        .border(1.dp, StatusWarning.copy(alpha = 0.4f))
                        .clickable { onDemoBannerClick() }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StatusWarning)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEMO MODE",
                                color = DeepObsidianBg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "Simulated Network Data Active",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Switch Mode",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceGlass)
                            .border(1.dp, NeonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "NetPulse Logo",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "NETPULSE",
                                color = TextPrimary,
                                fontWeight = FontWeight.Black,
                                fontSize = 17.sp,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(StatusOnline)
                            )
                        }
                        Text(
                            text = "Gateway: $gatewayIp",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onAdminLockClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAdminUnlocked) StatusOnline.copy(alpha = 0.2f) else SurfaceGlassElevated
                            )
                    ) {
                        Icon(
                            imageVector = if (isAdminUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Admin Gate Lock",
                            tint = if (isAdminUnlocked) StatusOnline else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

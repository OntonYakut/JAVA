package asteros.data;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import ru.asteros.vp.AsterosPrompt;
import ru.asteros.vp.Session;
import ru.asteros.vp.data.CommonClass;
import asteros.LocalUtility;

import com.avaya.sce.runtimecommon.SCESession;

import flow.IProjectVariables;

public class Client extends CommonClass {
	
	public String ani;
	public String dnis;
	private String pin_code;
	private String password;
	public String sessionID;
	
	final private static String ddVar = IProjectVariables.CLIENT__OBJECT;
	
	public Client() {
		super(null, ddVar);
	}
	
	public Client(SCESession mySession, String pin_code, String password){
		super(mySession, ddVar);
		this.ani = mySession.getVariableField(IProjectVariables.SESSION, IProjectVariables.SESSION_FIELD_ANI).getStringValue();
		this.dnis = mySession.getVariableField(IProjectVariables.SESSION, IProjectVariables.SESSION_FIELD_DNIS).getStringValue();
		this.sessionID = Session.getSessionId(mySession);
		this.pin_code = pin_code;
		this.password = password;
	}
	
	/**
	 * Получить отчет по панелям/разделам на объекте клиента
	 * @return PanelReport
	 */
	public PanelReport getPanelReport(){
		// Получаем отчет из БД
		String panel_text = DataBase.pr_ivr_panel_so_armed(mySession, pin_code);
		// Парсим его в объект-луковицу
		PanelReport panelReport = PanelReport.parsePanelReport(panel_text);
		
		return panelReport;
	}
	
	/**
	 * Авторизируем клиента по введенным данным
	 * @return responseCode
	 */
	public int authorizeClient(int mode){
		int responseCode = 0;
		
		//Проверяем авторизацию
		responseCode = DataBase.callCheckPassword(mySession, pin_code, password, mode, ani, sessionID);
		
		return responseCode;
	}
	
	/**
	 * Проверить прохождение сигнала от тревожной кнопки
	 * @param tag_end
	 * @return responseCode
	 */
	public int checkAlarmSignal(boolean tag_end){
		
		//Проверяем пришел ли сигнал
		// признак поступления сигнала = 1 - поступил, о - нет
		int responseCode = DataBase.pr_ivr_check_ktc(mySession, pin_code, tag_end);
		
		return responseCode;
	}
	
	/**
	 * Выполняем процедуру закрытия звонка
	 * @param mode
	 */
	public void closeConnect(int mode){
		//@mode - режим подключения (1 - простой, 2 - КТС)
		DataBase.pr_ivr_connect_off(mySession, pin_code, mode);
	}
	
	/**
	 * Сформировать asterosPrompt с отчетом
	 * @param mySession
	 * @param panelReport
	 * @return AsterosPrompt
	 */
	public static AsterosPrompt formPanelReport(SCESession mySession, PanelReport panelReport){
		
		AsterosPrompt asterosPrompt = LocalUtility.getAsterosPrompt(mySession);
		
		asterosPrompt.addUrlFile("onYourObject");// На вашем бъекте:
		
		HashMap<Integer, HashMap> panels = panelReport.panels;
		
		// Цикл панелей
		for (int i = 1; i <= panelReport.panelCount; i++) {
			HashMap<Integer, Integer> sections = panels.get(i);
			asterosPrompt.addUrlFile("panel_" + i); // Панель №
			// Цикл разделов
			Set keySet = sections.keySet();
			for (Iterator iterator = keySet.iterator(); iterator.hasNext();) {
				Integer key = (Integer) iterator.next();
				asterosPrompt.addUrlFile("section_" + key); // № раздел
				
				int status = sections.get(key);
				String statusPhrase = (status == 1) ? "objectProtected" : "objectDisarmed";
				asterosPrompt.addUrlFile(statusPhrase);
			}
			
		}
		return asterosPrompt;
	}
	
	
	public static Client load(SCESession mySession) {
		Client client = Client.load(mySession, Client.class, ddVar);
		return client;
	}
	
}

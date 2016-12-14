package cn.didadu;

import com.google.gson.Gson;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Subdivision;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by zhangjing on 16-12-12.
 */

@Service
public class IPService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 通过.将ip分割成四段
     * 第一段左移动24位，第二段左移动16位，第三段移8位转换成占满32位的int，包括符号位
     * 和long最大值进行&操作，变成无符号整数
     * @param ipAddress
     * @return
     */
    public long ipToScore(String ipAddress) {
        int score = 0;
        for (String v : ipAddress.split("\\.")){
            score = score * 256 + Integer.parseInt(v, 10);
        }
        return score&0x0FFFFFFFFL;
    }

    /**
     * 导入GeonameId
     * @param file
     */
    public void importIpsToRedis(File file) throws Exception{
        FileReader reader = new FileReader(file);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        CSVRecord csvRecord;
        Iterator<CSVRecord> recordIterator = parser.iterator();
        while (recordIterator.hasNext()){
            csvRecord = recordIterator.next();
            if(csvRecord.getRecordNumber() > 1){
                long score = ipToScore(csvRecord.get(0).split("/")[0]);
                //多个IP地址范围可能会被映射至同一个城市ID，所以加上行号确保member的唯一性
                String geonameId = csvRecord.get(1) + '_' + csvRecord.getRecordNumber();
                stringRedisTemplate.opsForZSet().add("ip_geonameId:", geonameId, score);
            }
        }
    }

    /**
     * 导入实际信息
     * @param file
     */
    public void importGeonameToRedis(File file) throws Exception {
        FileReader reader = new FileReader(file);
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
        CSVRecord csvRecord;
        Iterator<CSVRecord> recordIterator = parser.iterator();
        Gson gson = new Gson();
        while (recordIterator.hasNext()){
            csvRecord = recordIterator.next();
            String geonameId = csvRecord.get(0);
            String country = csvRecord.get(5);
            String region = csvRecord.get(3);
            String city = !StringUtils.isEmpty(csvRecord.get(10)) ? csvRecord.get(10) : csvRecord.get(7);
            String json = gson.toJson(new String[]{city, region, country});
            stringRedisTemplate.opsForHash().put("geonameId_city:", geonameId, json);
        }
    }

    /**
     * 通过ip查找城市
     * @param ipAddress
     * @return
     */
    public String[] findCityByIp(String ipAddress) {
        long score = ipToScore(ipAddress);
        Set<String> results = stringRedisTemplate.opsForZSet()
                .reverseRangeByScore("ip_geonameId:", 0, score, 0, 1);
        if (results.size() == 0) {
            return null;
        }
        String cityId = results.iterator().next();
        cityId = cityId.substring(0, cityId.indexOf('_'));
        return new Gson().fromJson(
                (String) stringRedisTemplate.opsForHash().get("geonameId_city:", cityId), String[].class);
    }

    /**
     * maxmind api
     * @param ipAddressStr
     * @throws Exception
     */
    public void findCityByApi(String ipAddressStr) throws Exception {
        File database = new File("/home/zhangjing/Desktop/ip/GeoLite2-City.mmdb");
        DatabaseReader reader = new DatabaseReader.Builder(database).build();
        InetAddress ipAddress = InetAddress.getByName(ipAddressStr);
        CityResponse response = reader.city(ipAddress);
        Country country = response.getCountry();
        System.out.println(country.getNames().get("zh-CN"));

        Subdivision subdivision = response.getMostSpecificSubdivision();
        System.out.println(subdivision.getName());    // 'Minnesota'
        System.out.println(subdivision.getIsoCode()); // 'MN'

        City city = response.getCity();
        System.out.println(city.getName()); // 'Minneapolis'
    }

}

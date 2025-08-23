-- REGION 추가
insert into regions(regionId, nameKo, nameEn) values
(1, '서울', 'Seoul'),
(2, '부산', 'Busan');

-- PLACE 추가
-- k-pop : 서울
insert into places(regionId, nameKo, nameEn, descriptionKo, descriptionEn, longitude, latitude, themes) values
(1, 'SMTOWN 코엑스 아티움', 'SMTOWN COEX Artium', 'SM 엔터테인먼트의 복합 문화 공간으로, K-POP 팬들에게 인기 있는 장소입니다.', 'A complex cultural space of SM Entertainment, popular among K-POP fans.', 127.059, 37.511, 'k-pop'),
(1, 'YG 엔터테인먼트', 'YG Entertainment', '빅뱅, 블랙핑크 등 유명 아티스트들이 소속된 YG 엔터테인먼트 본사입니다.', 'The headquarters of YG Entertainment, home to famous artists like Big Bang and BLACKPINK.', 126.924, 37.508, 'k-pop'),
(1, 'JYP 엔터테인먼트', 'JYP Entertainment', '트와이스, ITZY 등 인기 그룹이 소속된 JYP 엔터테인먼트 본사입니다.', 'The headquarters of JYP Entertainment, home to popular groups like TWICE and ITZY.', 127.028, 37.501, 'k-pop'),
(1, '한류스타 거리', 'Hallyu Star Street', '서울 강남구에 위치한 한류 스타들의 거리로, 팬들이 자주 찾는 명소입니다.', 'A street in Gangnam-gu, Seoul, known for Hallyu stars and frequently visited by fans.', 127.027, 37.497, 'k-pop'),
(1, 'K-POP 박물관', 'K-POP Museum', 'K-POP의 역사와 문화를 전시하는 박물관으로, 다양한 전시물이 있습니다.', 'A museum showcasing the history and culture of K-POP with various exhibits.', 126.978, 37.5665, 'k-pop');

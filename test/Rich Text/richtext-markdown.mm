<map version="freeplane 1.11.1">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<node TEXT="TEST MAP" FOLDED="false" ID="ID_696401721" CREATED="1610381621824" MODIFIED="1674731908385" STYLE="oval">
<font SIZE="18"/>
<hook NAME="MapStyle">
    <properties edgeColorConfiguration="#808080ff,#ff0000ff,#0000ffff,#00ff00ff,#ff00ffff,#00ffffff,#7c0000ff,#00007cff,#007c00ff,#7c007cff,#007c7cff,#7c7c00ff" associatedTemplateLocation="template:/standard-1.6.mm" fit_to_viewport="false" show_note_icons="true"/>

<map_styles>
<stylenode LOCALIZED_TEXT="styles.root_node" STYLE="oval" UNIFORM_SHAPE="true" VGAP_QUANTITY="24 pt">
<font SIZE="24"/>
<stylenode LOCALIZED_TEXT="styles.predefined" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="default" ID="ID_271890427" ICON_SIZE="12 pt" COLOR="#000000" STYLE="fork">
<arrowlink SHAPE="CUBIC_CURVE" COLOR="#000000" WIDTH="2" TRANSPARENCY="200" DASH="" FONT_SIZE="9" FONT_FAMILY="SansSerif" DESTINATION="ID_271890427" STARTARROW="NONE" ENDARROW="DEFAULT"/>
<font NAME="SansSerif" SIZE="10" BOLD="false" ITALIC="false"/>
<richcontent CONTENT-TYPE="plain/auto" TYPE="DETAILS"/>
<richcontent TYPE="NOTE" CONTENT-TYPE="plain/auto"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.details"/>
<stylenode LOCALIZED_TEXT="defaultstyle.attributes">
<font SIZE="9"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.note" COLOR="#000000" BACKGROUND_COLOR="#ffffff" TEXT_ALIGN="LEFT"/>
<stylenode LOCALIZED_TEXT="defaultstyle.floating">
<edge STYLE="hide_edge"/>
<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>
</stylenode>
<stylenode LOCALIZED_TEXT="defaultstyle.selection" BACKGROUND_COLOR="#afd3f7" BORDER_COLOR_LIKE_EDGE="false" BORDER_COLOR="#afd3f7"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.user-defined" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="styles.topic" COLOR="#18898b" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subtopic" COLOR="#cc3300" STYLE="fork">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.subsubtopic" COLOR="#669900">
<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.important" ID="ID_67550811">
<icon BUILTIN="yes"/>
<arrowlink COLOR="#003399" TRANSPARENCY="255" DESTINATION="ID_67550811"/>
</stylenode>
</stylenode>
<stylenode LOCALIZED_TEXT="styles.AutomaticLayout" POSITION="bottom_or_right" STYLE="bubble">
<stylenode LOCALIZED_TEXT="AutomaticLayout.level.root" COLOR="#000000" STYLE="oval" SHAPE_HORIZONTAL_MARGIN="10 pt" SHAPE_VERTICAL_MARGIN="10 pt">
<font SIZE="18"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,1" COLOR="#0033ff">
<font SIZE="16"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,2" COLOR="#00b439">
<font SIZE="14"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,3" COLOR="#990000">
<font SIZE="12"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,4" COLOR="#111111">
<font SIZE="10"/>
</stylenode>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,5"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,6"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,7"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,8"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,9"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,10"/>
<stylenode LOCALIZED_TEXT="AutomaticLayout.level,11"/>
</stylenode>
</stylenode>
</map_styles>
</hook>
<hook NAME="AutomaticEdgeColor" COUNTER="3" RULE="ON_BRANCH_CREATION"/>
<node TEXT="Test" POSITION="bottom_or_right" ID="ID_630811681" CREATED="1674035545257" MODIFIED="1674035549690">
<edge COLOR="#0000ff"/>
<node TEXT="Child 1 with details" ID="ID_734194014" CREATED="1674035550134" MODIFIED="1674731933081"><richcontent CONTENT-TYPE="xml/" TYPE="DETAILS">
<html>
  <head>
    
  </head>
  <body>
    <p>
      Some details
    </p>
  </body>
</html>
</richcontent>
</node>
<node TEXT="Child 2" ID="ID_749163868" CREATED="1674035553856" MODIFIED="1674731830862">
<node TEXT="https://github.com/freeplane/freeplane/discussions/" ID="ID_99748537" CREATED="1674731936105" MODIFIED="1674731963065"/>
</node>
<node TEXT="Child 3 with markdown note" ID="ID_1632614895" CREATED="1674035556389" MODIFIED="1674732447326"><richcontent TYPE="NOTE" CONTENT-TYPE="xml/markdown">
<html>
  <head>
    
  </head>
  <body>
    <p>
      ## Markdown
    </p>
    <p>
      
    </p>
    <p>
      Test **some text**
    </p>
    <p>
      
    </p>
    <p>
      - test 1
    </p>
    <p>
      &#xa0;- test 2
    </p>
    <p>
      - test 3
    </p>
  </body>
</html>
</richcontent>
</node>
<node TEXT="Child 4 with text" ID="ID_30451442" CREATED="1674035559658" MODIFIED="1674731878462"><richcontent TYPE="NOTE" CONTENT-TYPE="xml/">
<html>
  <head>
    
  </head>
  <body>
    <p style="margin-top: 0px; margin-right: 0px; margin-bottom: 15px; margin-left: 0px; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px; text-align: justify; color: rgb(0, 0, 0); font-family: Open Sans, Arial, sans-serif; font-size: 14px; font-style: normal; font-weight: 400; letter-spacing: normal; text-indent: 0px; text-transform: none; white-space: normal; word-spacing: 0px; background-color: rgb(255, 255, 255)">
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque dignissim, nisl sit amet sodales sagittis, nunc libero aliquet libero, eget elementum nulla velit in nisl. Vivamus non nunc hendrerit, sodales lacus vel, blandit orci. Aenean aliquam enim nec molestie fringilla. Vestibulum nec nunc sed urna porta convallis. Aliquam vitae ipsum eget metus convallis consectetur. Mauris varius nibh arcu, a fermentum est ultricies sit amet. Integer eu turpis quis tellus aliquet faucibus. Maecenas sed dignissim nisi. Nulla posuere, lorem eu facilisis venenatis, ex risus ornare dolor, sit amet pharetra arcu purus eu erat. Vestibulum ipsum ipsum, molestie non arcu et, faucibus commodo sapien. Phasellus ultrices, erat et vehicula congue, erat est vehicula sapien, eu venenatis lectus mauris blandit magna. Maecenas egestas augue est, eget feugiat ligula mollis eu. Vestibulum ullamcorper neque et tellus bibendum pellentesque.
    </p>
    <p style="margin-top: 0px; margin-right: 0px; margin-bottom: 15px; margin-left: 0px; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px; text-align: justify; color: rgb(0, 0, 0); font-family: Open Sans, Arial, sans-serif; font-size: 14px; font-style: normal; font-weight: 400; letter-spacing: normal; text-indent: 0px; text-transform: none; white-space: normal; word-spacing: 0px; background-color: rgb(255, 255, 255)">
      Curabitur ex mi, viverra non sem vitae, consequat porta velit. Aenean non odio ac urna ultricies porta. Suspendisse odio sapien, lobortis posuere porta a, scelerisque et ante. Vivamus nec nisl in massa rutrum elementum. Morbi efficitur est vel dolor auctor, id convallis felis tempus. Etiam ullamcorper malesuada cursus. Vestibulum venenatis ante nunc, ut lobortis ipsum porta sit amet. Etiam est nisl, posuere ac sodales at, suscipit auctor arcu. Praesent in molestie ligula. Etiam fermentum, tortor sed scelerisque molestie, eros elit ultricies dui, sed dignissim nunc libero vel tortor. Duis dapibus risus mauris, nec tristique magna mollis mattis.
    </p>
    <p style="margin-top: 0px; margin-right: 0px; margin-bottom: 15px; margin-left: 0px; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px; text-align: justify; color: rgb(0, 0, 0); font-family: Open Sans, Arial, sans-serif; font-size: 14px; font-style: normal; font-weight: 400; letter-spacing: normal; text-indent: 0px; text-transform: none; white-space: normal; word-spacing: 0px; background-color: rgb(255, 255, 255)">
      Sed venenatis, libero vel lobortis tempus, ligula sem vehicula justo, sed convallis velit dui eu diam. Phasellus varius, quam sed condimentum congue, neque metus rutrum ante, a bibendum diam velit a quam. Integer consectetur laoreet elit, nec laoreet lacus fermentum eget. Nam interdum placerat ligula, ut venenatis turpis. Cras justo arcu, egestas et viverra non, eleifend et tortor. Donec fringilla eros non justo gravida posuere. Donec tincidunt turpis lorem, et viverra sem scelerisque eu. Phasellus justo tellus, vulputate a eleifend vitae, pretium ac magna. Aliquam a sodales libero. Fusce ligula tellus, vestibulum vitae justo at, molestie congue dui. Praesent ac enim nec erat suscipit mattis eu ut libero. Suspendisse pretium placerat augue, at egestas ligula auctor sed. Duis varius eros scelerisque, gravida tortor eu, iaculis tellus. Mauris venenatis vehicula leo vel tristique. Nullam hendrerit nunc lorem, vel rutrum diam mollis in. Donec placerat suscipit ligula.
    </p>
    <p style="margin-top: 0px; margin-right: 0px; margin-bottom: 15px; margin-left: 0px; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px; text-align: justify; color: rgb(0, 0, 0); font-family: Open Sans, Arial, sans-serif; font-size: 14px; font-style: normal; font-weight: 400; letter-spacing: normal; text-indent: 0px; text-transform: none; white-space: normal; word-spacing: 0px; background-color: rgb(255, 255, 255)">
      Curabitur nisi arcu, consectetur sit amet metus sed, pulvinar tincidunt quam. Fusce eu accumsan ligula. In metus tellus, porttitor in mattis in, elementum quis dui. Suspendisse eget condimentum nibh. In in nisi massa. Pellentesque pharetra massa risus, a faucibus augue dignissim non. Maecenas ac luctus felis. Mauris feugiat elit sed tellus mollis ultrices. Proin efficitur sit amet nunc at facilisis. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Integer cursus nulla id augue fermentum ornare. Mauris non tortor non ante ultricies accumsan. Cras suscipit nulla eu quam pharetra imperdiet. Sed a mattis est. Praesent lacinia lectus quam, id placerat libero congue dignissim.
    </p>
    <p style="margin-top: 0px; margin-right: 0px; margin-bottom: 15px; margin-left: 0px; padding-top: 0px; padding-right: 0px; padding-bottom: 0px; padding-left: 0px; text-align: justify; color: rgb(0, 0, 0); font-family: Open Sans, Arial, sans-serif; font-size: 14px; font-style: normal; font-weight: 400; letter-spacing: normal; text-indent: 0px; text-transform: none; white-space: normal; word-spacing: 0px; background-color: rgb(255, 255, 255)">
      Pellentesque mauris metus, eleifend id interdum ac, dictum et purus. Sed facilisis augue sit amet massa mollis ullamcorper. Quisque nec nulla sem. Etiam tincidunt fermentum diam at vehicula. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Nunc tristique tempus hendrerit. Sed aliquam eget augue et aliquam. Vestibulum aliquet, diam non faucibus varius, enim nulla pulvinar magna, a luctus purus ante sed nisi.
    </p>
  </body>
</html>
</richcontent>
</node>
</node>
</node>
</map>

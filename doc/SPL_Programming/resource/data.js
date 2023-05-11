var tree = new MzTreeView("tree");
tree.setIconPath("images/");
 tree.nodes["0_1"] = "text:";
 tree.nodes["1_3"] = "text:Preface;url:topics/1.html";
 tree.nodes["1_4"] = "text:1 Doing arithmetic;url:topics/2.html";
 tree.nodes["4_5"] = "text:1.1 Data;url:topics/3.html";
 tree.nodes["4_6"] = "text:1.2 Variables and statements;url:topics/4.html";
 tree.nodes["4_7"] = "text:1.3 Functions;url:topics/5.html";
 tree.nodes["1_8"] = "text:2 Doing judgement;url:topics/6.html";
 tree.nodes["8_9"] = "text:2.1 Logic operation;url:topics/7.html";
 tree.nodes["8_10"] = "text:2.2 Branching structure;url:topics/8.html";
 tree.nodes["8_11"] = "text:2.3 Comments and jumps;url:topics/9.html";
 tree.nodes["1_12"] = "text:3 Doing loop;url:topics/10.html";
 tree.nodes["12_13"] = "text:3.1 Single layer loop;url:topics/11.html";
 tree.nodes["12_14"] = "text:3.2 Multilayer loop;url:topics/12.html";
 tree.nodes["12_15"] = "text:3.3 Conditional loop;url:topics/13.html";
 tree.nodes["12_16"] = "text:3.4 Endless loop;url:topics/14.html";
 tree.nodes["1_17"] = "text:4 Sequence;url:topics/15.html";
 tree.nodes["17_18"] = "text:4.1 Sequence;url:topics/16.html";
 tree.nodes["17_19"] = "text:4.2 Loop of sequence;url:topics/17.html";
 tree.nodes["17_20"] = "text:4.3 Multi-layer sequence;url:topics/18.html";
 tree.nodes["17_21"] = "text:4.4 Understanding objects;url:topics/19.html";
 tree.nodes["1_22"] = "text:5 Sequence as a whole;url:topics/20.html";
 tree.nodes["22_23"] = "text:5.1 Set operations;url:topics/21.html";
 tree.nodes["22_24"] = "text:5.2 Loop functions;url:topics/22.html";
 tree.nodes["22_25"] = "text:5.3 Loop functions: advanced;url:topics/23.html";
 tree.nodes["22_26"] = "text:5.4 Iterative function *;url:topics/24.html";
 tree.nodes["22_27"] = "text:5.5 Positioning and selection;url:topics/25.html";
 tree.nodes["22_28"] = "text:5.6 Sorting related;url:topics/26.html";
 tree.nodes["22_29"] = "text:5.7 Lambda syntax *;url:topics/27.html";
 tree.nodes["1_30"] = "text:6 Reuse;url:topics/28.html";
 tree.nodes["30_31"] = "text:6.1 User-Defined Functions;url:topics/29.html";
 tree.nodes["30_32"] = "text:6.2 Recursion *;url:topics/30.html";
 tree.nodes["30_33"] = "text:6.3 Reusable script;url:topics/31.html";
 tree.nodes["1_34"] = "text:7 String and time;url:topics/32.html";
 tree.nodes["34_35"] = "text:7.1 String;url:topics/33.html";
 tree.nodes["34_36"] = "text:7.2 Split and concatenate;url:topics/34.html";
 tree.nodes["34_37"] = "text:7.3 Date and time;url:topics/35.html";
 tree.nodes["1_38"] = "text:8 Data table;url:topics/36.html";
 tree.nodes["38_39"] = "text:8.1 Structured data;url:topics/37.html";
 tree.nodes["38_40"] = "text:8.2 Table sequence and record sequence;url:topics/38.html";
 tree.nodes["38_41"] = "text:8.3 Generation of table sequence;url:topics/39.html";
 tree.nodes["38_42"] = "text:8.4 Loop functions;url:topics/40.html";
 tree.nodes["38_43"] = "text:8.5 Calculations on the fields;url:topics/41.html";
 tree.nodes["1_44"] = "text:9 Grouping;url:topics/42.html";
 tree.nodes["44_45"] = "text:9.1 Grouping and aggregation;url:topics/43.html";
 tree.nodes["44_46"] = "text:9.2 Enumeration and alignment;url:topics/44.html";
 tree.nodes["44_47"] = "text:9.3 Order-related grouping;url:topics/45.html";
 tree.nodes["44_48"] = "text:9.4 Expansion and transpose;url:topics/46.html";
 tree.nodes["1_49"] = "text:10 Association;url:topics/47.html";
 tree.nodes["49_50"] = "text:10.1 Primary key;url:topics/48.html";
 tree.nodes["49_51"] = "text:10.2 Foreign key;url:topics/49.html";
 tree.nodes["49_52"] = "text:10.3 Merge;url:topics/50.html";
 tree.nodes["49_53"] = "text:10.4 Join;url:topics/51.html";
 tree.nodes["1_54"] = "text:11 Big data;url:topics/52.html";
 tree.nodes["54_55"] = "text:11.1 Big data and cursor;url:topics/53.html";
 tree.nodes["54_56"] = "text:11.2 Fuctions on cursor;url:topics/54.html";
 tree.nodes["54_57"] = "text:11.3 Ordered cursor;url:topics/55.html";
 tree.nodes["54_58"] = "text:11.4 Big cursor;url:topics/56.html";
 tree.nodes["1_59"] = "text:12 Drawing graphics;url:topics/57.html";
 tree.nodes["59_60"] = "text:12.1 Canvas and elements;url:topics/58.html";
 tree.nodes["59_61"] = "text:12.2 Coordinate system;url:topics/59.html";
 tree.nodes["59_62"] = "text:12.3 More coordinate systems;url:topics/60.html";
 tree.nodes["59_63"] = "text:12.4 Legend;url:topics/61.html";
 tree.nodes["1_64"] = "text:Postscript;url:topics/62.html";
tree.setTarget("mainFrame");
document.write(tree.toString());